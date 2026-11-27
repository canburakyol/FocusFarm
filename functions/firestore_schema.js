// Firestore Schema for Referral System
// Collection: referrals
// Document ID: {referralCode}

{
  // Document: referrals/A3X9K2M7
  "referralCode": "A3X9K2M7",
  "referrerUserId": "user_12345",
  "createdAt": 1737072000000,
  "referredUsers": [
    {
      "userId": "user_67890",
      "referredAt": 1737075600000,
      "rewardGranted": true
    }
  ],
  "totalReferrals": 1,
  "totalRewardsGranted": 50
}

// Collection: users
// Document ID: {userId}

{
  // Document: users/user_12345
  "userId": "user_12345",
  "referralCode": "A3X9K2M7",
  "referredBy": null, // veya başka bir referralCode
  "seedBalance": 150,
  "totalReferrals": 1,
  "createdAt": 1737072000000
}
