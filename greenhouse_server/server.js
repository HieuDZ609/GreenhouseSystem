const admin = require("firebase-admin");
const serviceAccount = require("./securityAccountKey.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL:
    "https://greenhousesystem-97224-default-rtdb.asia-southeast1.firebasedatabase.app/",
});

const db = admin.database();
console.log("Server đang chạy và sẵn sàng lắng nghe...");

const notifRef = db.ref("/GreenHouseSystem/notifications");

notifRef.on("child_added", (userSnapshot) => {
  const uid = userSnapshot.key;

  // Tiếp tục lắng nghe các thông báo mới bên trong UID đó
  db.ref(`/GreenHouseSystem/notifications/${uid}`).on(
    "child_added",
    async (notifSnapshot) => {
      const notifData = notifSnapshot.val();

      // Nếu thông báo này không tồn tại hoặc đã xử lý rồi thì bỏ qua
      if (!notifData || notifData.fcmSent === true) return;

      console.log(
        `\n🔔 Phát hiện cảnh báo mới của User [${uid}]:`,
        notifData.title || "Cảnh báo hệ thống",
      );

      // 1. Lấy Object chứa danh sách Token của user từ Database
      const tokenSnapshot = await db
        .ref(`/GreenHouseSystem/users/${uid}/fcmToken`)
        .once("value");
      const tokensObj = tokenSnapshot.val();

      if (!tokensObj) {
        console.log(`User ${uid} chưa có FCM Token trên DB.`);
        return;
      }

      // 2. Chuyển đổi Object { "token_abc": true, "token_xyz": true } thành mảng [ "token_abc", "token_xyz" ]
      const tokenArray = Object.keys(tokensObj);

      if (tokenArray.length === 0) {
        console.log(`Danh sách thiết bị của User ${uid} đang rỗng.`);
        return;
      }

      // 3. Cấu hình nội dung FCM gửi đi (Dùng 'tokens' số nhiều nhận vào 1 mảng)
      const payload = {
        tokens: tokenArray,
        notification: {
          title: notifData.title || "CẢNH BÁO VƯỜN CÂY",
          body: notifData.message || "Phát hiện vượt ngưỡng an toàn!",
        },
      };

      // 4. Tiến hành bắn thông báo đến toàn bộ thiết bị
      try {
        const response = await admin.messaging().sendEachForMulticast(payload);
        console.log(
          `Đã bắn FCM thành công cho ${response.successCount}/${tokenArray.length} thiết bị.`,
        );

        await notifSnapshot.ref.update({ fcmSent: true });
        console.log(
          `Đã dọn dẹp thông báo cũ của User [${uid}] trên Realtime DB.`,
        );
      } catch (error) {
        console.error("Lỗi trong quá trình bắn FCM:", error);
      }
    },
  );
});
