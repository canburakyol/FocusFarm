const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();
const db = admin.firestore();

exports.verifyReferral = functions.https.onCall(async (data, context) => {
  // Authentication kontrolü
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'Kullanıcı giriş yapmamış.'
    );
  }

  const { referralCode } = data;
  const currentUserId = context.auth.uid;

  if (!referralCode) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'Referral kodu gerekli.'
    );
  }

  try {
    // Tüm işlemleri tek bir Transaction içinde yönetiyoruz (SOTA Veri Tutarlılığı)
    await db.runTransaction(async (transaction) => {
      // 1. Transaction içi OKUMALAR (Tüm okumalar yazmalardan önce gelmelidir)
      const referralRef = db.collection('referrals').doc(referralCode);
      const referralDocSnapshot = await transaction.get(referralRef);

      if (!referralDocSnapshot.exists) {
        throw new functions.https.HttpsError(
          'not-found',
          'Geçersiz referral kodu.'
        );
      }

      const referralData = referralDocSnapshot.data();
      const referrerUserId = referralData.referrerUserId;

      // Kendi kodunu kullanamazsın
      if (referrerUserId === currentUserId) {
        throw new functions.https.HttpsError(
          'already-exists',
          'Kendi referral kodunu kullanamazsın.'
        );
      }

      // Referrer dokümanını çek
      const referrerRef = db.collection('users').doc(referrerUserId);
      const referrerDocSnapshot = await transaction.get(referrerRef);

      if (!referrerDocSnapshot.exists) {
        throw new functions.https.HttpsError('not-found', 'Referrer bulunamadı.');
      }

      // Mevcut kullanıcıyı çek
      const userRef = db.collection('users').doc(currentUserId);
      const userDocSnapshot = await transaction.get(userRef);

      if (userDocSnapshot.exists) {
        const userData = userDocSnapshot.data();
        if (userData.referredBy) {
          throw new functions.https.HttpsError(
            'already-exists',
            'Zaten bir referral kodu kullanmışsın.'
          );
        }
      }

      // 2. Transaction içi YAZMALAR
      const currentBalance = referrerDocSnapshot.data().seedBalance || 0;
      const newBalance = currentBalance + 50;

      // Referrer ödülünü ekle ve sayacını artır
      transaction.update(referrerRef, {
        seedBalance: newBalance,
        totalReferrals: admin.firestore.FieldValue.increment(1),
      });

      // Davet belgesini güncelle
      transaction.update(referralRef, {
        referredUsers: admin.firestore.FieldValue.arrayUnion({
          userId: currentUserId,
          referredAt: admin.firestore.FieldValue.serverTimestamp(),
          rewardGranted: true,
        }),
        totalReferrals: admin.firestore.FieldValue.increment(1),
        totalRewardsGranted: admin.firestore.FieldValue.increment(50),
      });

      if (userDocSnapshot.exists) {
        // Kullanıcı belgesi zaten varsa (Auth onCreate tetiklenmişse) referredBy ve seed güncelle
        transaction.update(userRef, {
          referredBy: referralCode,
          seedBalance: admin.firestore.FieldValue.increment(50), // Yeni kullanıcı da 50 seed kazansın
        });
      } else {
        // Auth onCreate henüz tetiklenmediyse (yarış durumu), yeni kullanıcıyı kendimiz oluştururuz:
        // SOTA: Arkadaş kodunu kopyalamak yerine YENİ bir benzersiz kod oluşturuyoruz
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
        let newCode = '';
        for (let i = 0; i < 8; i++) {
          newCode += chars.charAt(Math.floor(Math.random() * chars.length));
        }

        const newReferralRef = db.collection('referrals').doc(newCode);

        transaction.set(userRef, {
          userId: currentUserId,
          referralCode: newCode,
          referredBy: referralCode,
          seedBalance: 50, // 50 seed başlangıç ödülü
          totalReferrals: 0,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        transaction.set(newReferralRef, {
          referralCode: newCode,
          referrerUserId: currentUserId,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          referredUsers: [],
          totalReferrals: 0,
          totalRewardsGranted: 0,
        });
      }
    });

    // Log referral verification
    functions.logger.info('Referral verified', {
      referrerUserId: referralCode, // Code itself identifies the path
      referredUserId: currentUserId,
      referralCode: referralCode,
    });

    return {
      success: true,
      message: 'Referral başarıyla doğrulandı. Her iki taraf da 50 seed kazandı.',
      rewardAmount: 50,
    };

  } catch (error) {
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }

    functions.logger.error('Referral verification error:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Referral doğrulanırken bir hata oluştu.'
    );
  }
});

// Kullanıcı ilk kez giriş yaptığında referral code oluştur
exports.createUserReferralCode = functions.auth.user().onCreate(async (user) => {
  const referralCode = await generateReferralCode();

  await db.collection('users').doc(user.uid).set({
    userId: user.uid,
    referralCode: referralCode,
    referredBy: null,
    seedBalance: 0,
    totalReferrals: 0,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  await db.collection('referrals').doc(referralCode).set({
    referralCode: referralCode,
    referrerUserId: user.uid,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    referredUsers: [],
    totalReferrals: 0,
    totalRewardsGranted: 0,
  });

  return null;
});

async function generateReferralCode() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  let code;
  let exists;
  do {
    code = '';
    for (let i = 0; i < 8; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    const doc = await db.collection('referrals').doc(code).get();
    exists = doc.exists;
  } while (exists);
  return code;
}

// Kullanıcı silindiğinde veritabanındaki bilgilerini temizle (Google Play Veri Güvenliği Politikası)
exports.deleteUserUserData = functions.auth.user().onDelete(async (user) => {
  const userId = user.uid;
  try {
    const userDoc = await db.collection('users').doc(userId).get();
    if (userDoc.exists) {
      const userData = userDoc.data();
      const referralCode = userData.referralCode;
      
      if (referralCode) {
        await db.collection('referrals').doc(referralCode).delete();
      }
      await db.collection('users').doc(userId).delete();
    }
    functions.logger.info(`Successfully deleted user data for user ${userId}`);
  } catch (error) {
    functions.logger.error(`Error deleting user data for user ${userId}:`, error);
  }
  return null;
});
