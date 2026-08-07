package com.example.myapplication.utils

/**
 * Singleton Mapper giúp chuyển đổi tất cả thông báo lỗi từ Backend (BE) dạng tiếng Anh
 * cũng như các thông báo từ Frontend (FE) sang tiếng Việt thống nhất và thân thiện.
 */
object ErrorMessageMapper {

    fun map(rawError: String?, httpStatusCode: Int? = null): String {
        if (rawError.isNullOrBlank()) {
            return mapByStatusCode(httpStatusCode)
        }

        val trimmed = rawError.trim()

        // 1. General & Invalid (Lỗi hệ thống & Kiểm tra định dạng)
        when {
            trimmed.contains("Something went wrong, please try again later", ignoreCase = true) ->
                return "Đã có lỗi xảy ra, vui lòng thử lại sau"
            trimmed.contains("Sorry, you needs to provide authentication credentials to access this resource", ignoreCase = true) ||
            trimmed.contains("authentication credentials", ignoreCase = true) ->
                return "Xin lỗi, bạn cần cung cấp thông tin xác thực để truy cập tài nguyên này"
            trimmed.contains("Sorry, you do not have the necessary permissions to access this resource", ignoreCase = true) ||
            trimmed.contains("necessary permissions", ignoreCase = true) ->
                return "Xin lỗi, bạn không có quyền truy cập tài nguyên này"
            trimmed.contains("You do not have permission to update or delete this resource", ignoreCase = true) ||
            trimmed.contains("permission to update or delete", ignoreCase = true) ->
                return "Bạn không có quyền cập nhật hoặc xóa tài nguyên này"
            trimmed.contains("This field is invalid", ignoreCase = true) ->
                return "Trường này không hợp lệ"
            trimmed.contains("Invalid format", ignoreCase = true) ->
                return "Sai định dạng"
            trimmed.contains("This field is required", ignoreCase = true) ->
                return "Trường này là bắt buộc"
            trimmed.contains("This field can't be blank", ignoreCase = true) ->
                return "Trường này không được để trống"
            trimmed.contains("Unsatisfactory password", ignoreCase = true) ||
            trimmed.contains("Password must include letters, numbers, and special characters", ignoreCase = true) ->
                return "Mật khẩu không đạt yêu cầu (phải bao gồm cả chữ, số và ký tự đặc biệt)"
            trimmed.contains("Invalid email format", ignoreCase = true) ->
                return "Định dạng email không hợp lệ"
            trimmed.contains("Wrong or Invalid datetime picker", ignoreCase = true) ->
                return "Sai định dạng ngày tháng"
            trimmed.contains("DateTime must be format yyyy-MM-dd HH:mm:ss", ignoreCase = true) ->
                return "Ngày giờ phải theo định dạng yyyy-MM-dd HH:mm:ss"
            trimmed.contains("Date must format yyyy-MM-dd and greater than current date", ignoreCase = true) ->
                return "Ngày phải lớn hơn ngày hiện tại và theo định dạng yyyy-MM-dd"
            trimmed.contains("Date of birth must be in the past and format yyyy-MM-dd", ignoreCase = true) ->
                return "Ngày sinh phải là ngày trong quá khứ và theo định dạng yyyy-MM-dd"
            trimmed.contains("Only PNG, JPG, JPEG, WEBP or GIF images are allowed", ignoreCase = true) ->
                return "Chỉ cho phép hình ảnh PNG, JPG, JPEG, WEBP hoặc GIF"
        }

        // 2. Auth & Register (Xác thực & Tài khoản)
        when {
            trimmed.contains("Username is incorrect", ignoreCase = true) ->
                return "Tên đăng nhập không chính xác"
            trimmed.contains("Password is incorrect", ignoreCase = true) ->
                return "Mật khẩu không chính xác"
            trimmed.contains("Old password is incorrect or new passwords do not match", ignoreCase = true) ->
                return "Mật khẩu cũ không chính xác hoặc mật khẩu mới không khớp"
            trimmed.contains("Password confirmation does not match!", ignoreCase = true) ||
            trimmed.contains("Password confirmation does not match", ignoreCase = true) ->
                return "Mật khẩu xác nhận không khớp!"
            trimmed.contains("Invalid refresh token", ignoreCase = true) ||
            trimmed.contains("Refresh token was expired", ignoreCase = true) ->
                return "Refresh token không hợp lệ hoặc đã hết hạn"
            trimmed.contains("This email is already in use", ignoreCase = true) ->
                return "Email này đã được sử dụng!"
            trimmed.contains("This account has already been verified!", ignoreCase = true) ||
            trimmed.contains("This account has already been verified", ignoreCase = true) ->
                return "Tài khoản này đã được xác thực trước đó!"
            trimmed.contains("Account has not been verified. Please check your email to activate it!", ignoreCase = true) ||
            trimmed.contains("Account has not been verified", ignoreCase = true) ->
                return "Tài khoản chưa được xác thực. Vui lòng kiểm tra email để kích hoạt!"
            trimmed.contains("Only activated accounts can recover passwords. Please check your verification email first!", ignoreCase = true) ||
            trimmed.contains("Only activated accounts can recover passwords", ignoreCase = true) ->
                return "Chỉ tài khoản đã kích hoạt mới có thể khôi phục mật khẩu. Vui lòng kiểm tra email xác thực trước!"
            trimmed.contains("OTP code is incorrect or has expired", ignoreCase = true) ->
                return "Mã OTP không chính xác hoặc đã hết hạn"
            trimmed.contains("You are doing this too fast, please wait 60 seconds!", ignoreCase = true) ||
            trimmed.contains("please wait 60 seconds", ignoreCase = true) ->
                return "Bạn đang thao tác quá nhanh, vui lòng đợi 60 giây!"
            trimmed.contains("The email sending system is experiencing issues, please try again later!", ignoreCase = true) ||
            trimmed.contains("email sending system is experiencing issues", ignoreCase = true) ->
                return "Hệ thống gửi mail đang gặp sự cố, vui lòng thử lại sau!"
            trimmed.contains("Password reset session has expired, please request OTP again", ignoreCase = true) ->
                return "Phiên đổi mật khẩu đã hết hạn, vui lòng yêu cầu lại OTP"
            trimmed.contains("The registration session has expired or the email does not exist. Please register again.", ignoreCase = true) ||
            trimmed.contains("registration session has expired", ignoreCase = true) ->
                return "Phiên đăng ký đã hết hạn hoặc email không tồn tại. Vui lòng đăng ký lại."
            trimmed.contains("A system error occurred while processing data. Please try again later.", ignoreCase = true) ->
                return "Lỗi hệ thống khi xử lý dữ liệu. Vui lòng thử lại sau."
        }

        // 3. Google Auth (Đăng nhập bằng Google)
        when {
            trimmed.contains("Invalid or expired Firebase token", ignoreCase = true) ->
                return "Token Firebase không hợp lệ hoặc đã hết hạn"
            trimmed.contains("Google login failed. Please try again", ignoreCase = true) ||
            trimmed.contains("Google login failed", ignoreCase = true) ->
                return "Đăng nhập Google thất bại. Vui lòng thử lại"
            trimmed.contains("Please complete your profile before using", ignoreCase = true) ->
                return "Vui lòng hoàn thiện thông tin cá nhân trước khi sử dụng"
            trimmed.contains("Profile has already been completed", ignoreCase = true) ->
                return "Hồ sơ đã được hoàn thiện rồi"
            trimmed.contains("Google login accounts do not support password changes", ignoreCase = true) ->
                return "Tài khoản đăng nhập bằng Google không hỗ trợ thay đổi mật khẩu"
        }

        // 4. Chat (Nhắn tin & Nhóm)
        when {
            trimmed.contains("Conversation not found", ignoreCase = true) ->
                return "Không tìm thấy cuộc trò chuyện"
            trimmed.contains("Message not found", ignoreCase = true) ->
                return "Không tìm thấy tin nhắn"
            trimmed.contains("Target conversation not found", ignoreCase = true) ->
                return "Không tìm thấy cuộc hội thoại đích"
            trimmed.contains("You are not a member of this conversation", ignoreCase = true) ->
                return "Bạn không phải là thành viên của cuộc trò chuyện này"
            trimmed.contains("You are not the sender of this message", ignoreCase = true) ->
                return "Bạn không phải là người gửi tin nhắn này"
            trimmed.contains("Group has reached the maximum number of members", ignoreCase = true) ->
                return "Nhóm đã đạt giới hạn số lượng thành viên tối đa"
            trimmed.contains("You have already left this group", ignoreCase = true) ->
                return "Bạn đã rời khỏi nhóm này"
            trimmed.contains("Only the group owner can perform this action", ignoreCase = true) ->
                return "Chỉ trưởng nhóm mới được thực hiện hành động này"
            trimmed.contains("Cannot dissolve a direct conversation", ignoreCase = true) ->
                return "Không thể giải tán cuộc hội thoại 1-1"
            trimmed.contains("Cannot transfer ownership to yourself", ignoreCase = true) ->
                return "Không thể chuyển quyền trưởng nhóm cho chính mình"
            trimmed.contains("This user has been blocked", ignoreCase = true) ->
                return "Người dùng này đã bị chặn"
            trimmed.contains("You have already blocked this user", ignoreCase = true) ->
                return "Bạn đã chặn người dùng này trước đó"
            trimmed.contains("Cannot block yourself", ignoreCase = true) ->
                return "Bạn không thể tự chặn chính mình"
            trimmed.contains("Cannot recall message after 30 minutes", ignoreCase = true) ->
                return "Không thể thu hồi tin nhắn sau 30 phút"
            trimmed.contains("You are not the author of this message", ignoreCase = true) ->
                return "Bạn không có quyền thu hồi tin nhắn này"
            trimmed.contains("Message recall time has expired", ignoreCase = true) ->
                return "Đã quá thời gian cho phép thu hồi tin nhắn"
            trimmed.contains("This message has already been recalled", ignoreCase = true) ->
                return "Tin nhắn này đã được thu hồi trước đó"
            trimmed.contains("This message is already pinned", ignoreCase = true) ->
                return "Tin nhắn này đã được ghim rồi"
            trimmed.contains("This message is not pinned", ignoreCase = true) ->
                return "Tin nhắn này chưa được ghim"
            trimmed.contains("Cannot forward a recalled message", ignoreCase = true) ->
                return "Không thể chuyển tiếp tin nhắn đã thu hồi"
            trimmed.contains("Cannot reply to a recalled message", ignoreCase = true) ->
                return "Không thể trả lời tin nhắn đã thu hồi"
            trimmed.contains("You cannot report yourself", ignoreCase = true) ->
                return "Bạn không thể tự báo cáo chính mình"
            trimmed.contains("Cannot create a conversation with yourself", ignoreCase = true) ->
                return "Không thể tạo cuộc trò chuyện với chính mình"
            trimmed.contains("Invalid file", ignoreCase = true) ->
                return "Tập tin không hợp lệ"
            trimmed.contains("File size exceeds the allowed limit", ignoreCase = true) ->
                return "Tập tin vượt quá kích thước cho phép"
        }

        // 5. Friendship (Kết bạn)
        when {
            trimmed.contains("You cannot send a friend request to yourself", ignoreCase = true) ->
                return "Bạn không thể tự gửi lời mời kết bạn cho chính mình"
            trimmed.contains("You are already friends with this user", ignoreCase = true) ->
                return "Bạn đã là bạn bè với người này rồi"
            trimmed.contains("You are not friends with this user", ignoreCase = true) ->
                return "Bạn không phải là bạn bè với người này"
            trimmed.contains("Friend request has already been sent", ignoreCase = true) ->
                return "Lời mời kết bạn đã được gửi trước đó"
            trimmed.contains("Friend request not found", ignoreCase = true) ->
                return "Không tìm thấy lời mời kết bạn"
            trimmed.contains("You are not the receiver of this friend request", ignoreCase = true) ->
                return "Bạn không phải là người nhận lời mời kết bạn này"
            trimmed.contains("No user found with this phone number", ignoreCase = true) ->
                return "Không tìm thấy người dùng với số điện thoại này"
            trimmed.contains("No user found matching the keyword", ignoreCase = true) ->
                return "Không tìm thấy người dùng với từ khóa này"
        }

        // 6. Booking (Đặt lịch hẹn)
        when {
            trimmed.contains("Booking can only be created in 1-1 conversations for now", ignoreCase = true) ||
            trimmed.contains("Cannot book in a group chat, direct messages only", ignoreCase = true) ->
                return "Hiện tại chỉ hỗ trợ đặt lịch hẹn trong cuộc trò chuyện 1-1"
            trimmed.contains("You have reached the maximum number of active bookings (3) in this conversation.", ignoreCase = true) ||
            trimmed.contains("maximum number of active bookings (3)", ignoreCase = true) ->
                return "Bạn đã vượt quá số lượng đặt lịch hẹn tối đa (3) trong cuộc trò chuyện này."
            trimmed.contains("You have reached the maximum number of active bookings", ignoreCase = true) ->
                return "Bạn đã đạt giới hạn số lượng lịch hẹn đang hoạt động"
            trimmed.contains("Could not find partner in conversation", ignoreCase = true) ->
                return "Không tìm thấy đối tác trong cuộc trò chuyện"
            trimmed.contains("You are not a participant in this booking", ignoreCase = true) ->
                return "Bạn không phải là người tham gia lịch hẹn này"
            trimmed.contains("You are not a member of this conversation", ignoreCase = true) ->
                return "Bạn không phải là thành viên của cuộc hội thoại này"
            trimmed.contains("Booking not found", ignoreCase = true) ->
                return "Không tìm thấy lịch hẹn"
            trimmed.contains("Booking is not in PENDING state", ignoreCase = true) ->
                return "Lịch hẹn không ở trạng thái PENDING"
            trimmed.contains("Invalid booking status for this action", ignoreCase = true) ->
                return "Trạng thái lịch hẹn không hợp lệ cho thao tác này"
            trimmed.contains("This booking has not been scheduled yet", ignoreCase = true) ->
                return "Lịch hẹn này chưa có thời gian diễn ra cụ thể"
            trimmed.contains("Scheduled time must be in the future", ignoreCase = true) ->
                return "Thời gian hẹn phải ở trong tương lai"
            trimmed.contains("Booking must be scheduled further in advance", ignoreCase = true) ->
                return "Lịch hẹn quá gấp, vui lòng đặt xa hơn"
            trimmed.contains("It is too late to cancel this booking", ignoreCase = true) ->
                return "Đã quá trễ để hủy lịch hẹn này"
            trimmed.contains("Reason is required for cancellation", ignoreCase = true) ->
                return "Vui lòng nhập lý do hủy lịch"
            trimmed.contains("Cannot complete the booking before its scheduled time", ignoreCase = true) ->
                return "Không thể hoàn thành trước giờ hẹn"
            trimmed.contains("You can only rate your partner", ignoreCase = true) ->
                return "Bạn chỉ có thể đánh giá đối tác của mình"
            trimmed.contains("You have already rated this booking", ignoreCase = true) ->
                return "Bạn đã đánh giá lịch hẹn này rồi"
            trimmed.contains("Rating score must be between 1 and 5", ignoreCase = true) ->
                return "Điểm đánh giá phải từ 1 đến 5 sao"
            trimmed.contains("Location information is required", ignoreCase = true) ->
                return "Yêu cầu nhập thông tin địa điểm"
            trimmed.contains("scheduledAt is required", ignoreCase = true) ->
                return "scheduledAt là bắt buộc"
            trimmed.contains("scheduledAt must be in the future", ignoreCase = true) ->
                return "scheduledAt phải ở trong tương lai"
            trimmed.contains("locationName is required", ignoreCase = true) ->
                return "locationName là bắt buộc"
            trimmed.contains("locationAddress is required", ignoreCase = true) ->
                return "locationAddress là bắt buộc"
            trimmed.contains("locationDistrict is required", ignoreCase = true) ->
                return "locationDistrict là bắt buộc"
            trimmed.contains("locationCity is required", ignoreCase = true) ->
                return "locationCity là bắt buộc"
            trimmed.contains("Failed to serialize booking payload.", ignoreCase = true) ->
                return "Lỗi khi chuyển đổi dữ liệu đặt lịch hẹn."
            trimmed.contains("Cannot book with a blocked user", ignoreCase = true) ->
                return "Không thể hẹn với người dùng đã bị chặn"
        }

        // 7. File (Tải lên tập tin)
        when {
            trimmed.contains("File size exceeds limit", ignoreCase = true) ->
                return "Kích thước file vượt quá giới hạn 10MB"
            trimmed.contains("File is empty", ignoreCase = true) ->
                return "File rỗng"
            trimmed.contains("File type not allowed", ignoreCase = true) ->
                return "Định dạng file không được hỗ trợ"
            trimmed.contains("Invalid file name", ignoreCase = true) ->
                return "Tên file không hợp lệ"
            trimmed.contains("Upload failed", ignoreCase = true) ->
                return "Tải lên file thất bại"
        }

        // 8. Location (Vị trí)
        when {
            trimmed.contains("User status is inactive", ignoreCase = true) ->
                return "Trạng thái người dùng không hoạt động"
            trimmed.contains("You must enable Buddy Active to update location", ignoreCase = true) ->
                return "Bạn cần bật chế độ Sẵn sàng để cập nhật vị trí"
            trimmed.contains("Invalid radar radius", ignoreCase = true) ->
                return "Bán kính quét không hợp lệ"
            trimmed.contains("Invalid GPS coordinates", ignoreCase = true) ->
                return "Tọa độ không hợp lệ"
        }

        // 9. Notification (Thông báo)
        when {
            trimmed.contains("Notification not found", ignoreCase = true) ->
                return "Không tìm thấy thông báo"
            trimmed.contains("You are not the recipient of this notification", ignoreCase = true) ->
                return "Bạn không phải là người nhận thông báo này"
            trimmed.contains("Notification ID list must not be empty", ignoreCase = true) ->
                return "Danh sách ID thông báo không được rỗng"
        }

        // 10. User & Role Search (Người dùng & Quyền)
        when {
            trimmed.contains("User not found with this username:", ignoreCase = true) ||
            trimmed.contains("User not found with username:", ignoreCase = true) -> {
                val username = trimmed.substringAfter(":").trim()
                return "Không tìm thấy người dùng nào với tên đăng nhập: $username"
            }
            trimmed.contains("User not found with id:", ignoreCase = true) -> {
                val id = trimmed.substringAfter(":").trim()
                return "Không tìm thấy người dùng nào với id: $id"
            }
            trimmed.contains("Role USER not found in the system!", ignoreCase = true) ||
            trimmed.contains("Role USER not found", ignoreCase = true) ->
                return "Chưa có quyền USER trong hệ thống!"
        }

        // Catch network exception messages
        if (trimmed.contains("Unable to resolve host", ignoreCase = true) ||
            trimmed.contains("Failed to connect", ignoreCase = true) ||
            trimmed.contains("timeout", ignoreCase = true) ||
            trimmed.contains("Connection refused", ignoreCase = true)
        ) {
            return "Không thể kết nối tới server. Vui lòng kiểm tra lại kết nối mạng!"
        }

        // Return original if already in Vietnamese
        if (isVietnamese(trimmed)) {
            return trimmed
        }

        return mapByStatusCode(httpStatusCode) ?: trimmed
    }

    private fun mapByStatusCode(statusCode: Int?): String {
        return when (statusCode) {
            400 -> "Yêu cầu không hợp lệ. Vui lòng kiểm tra lại thông tin"
            401 -> "Xin lỗi, bạn cần cung cấp thông tin xác thực để truy cập tài nguyên này"
            403 -> "Xin lỗi, bạn không có quyền truy cập tài nguyên này"
            404 -> "Không tìm thấy dữ liệu yêu cầu"
            409 -> "Dữ liệu đã tồn tại hoặc có xung đột xảy ra"
            429 -> "Bạn đang thao tác quá nhanh, vui lòng đợi 60 giây!"
            500, 502, 503, 504 -> "Đã có lỗi xảy ra từ hệ thống, vui lòng thử lại sau"
            else -> "Đã có lỗi xảy ra, vui lòng thử lại sau"
        }
    }

    private fun isVietnamese(text: String): Boolean {
        val vietnameseChars = "àáảãạâầấẩẫậăằắẳẵặèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđĐ"
        return vietnameseChars.any { text.contains(it, ignoreCase = true) }
    }
}
