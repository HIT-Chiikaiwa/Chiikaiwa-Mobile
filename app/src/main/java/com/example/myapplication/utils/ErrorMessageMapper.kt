package com.example.myapplication.utils

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Singleton Mapper giúp chuyển đổi tất cả thông báo lỗi từ Backend (BE) và Frontend (FE)
 * sang tiếng Việt một cách thống nhất và thân thiện với người dùng.
 */
object ErrorMessageMapper {

    fun map(rawError: String?, httpStatusCode: Int? = null): String {
        if (rawError.isNullOrBlank()) {
            return mapByStatusCode(httpStatusCode)
        }

        val trimmed = rawError.trim()

        // 1. Lỗi hệ thống chung và định dạng dữ liệu (General & Invalid)
        when {
            trimmed.contains("Unauthorized", ignoreCase = true) || trimmed.contains("unauthenticated", ignoreCase = true) ->
                return "Xin lỗi, bạn cần cung cấp thông tin xác thực để truy cập tài nguyên này"
            trimmed.contains("Forbidden", ignoreCase = true) || trimmed.contains("access denied", ignoreCase = true) ->
                return "Xin lỗi, bạn không có quyền truy cập tài nguyên này"
            trimmed.contains("don't have permission to update or delete", ignoreCase = true) ->
                return "Bạn không có quyền cập nhật hoặc xóa tài nguyên này"
            trimmed.contains("invalid field", ignoreCase = true) ->
                return "Trường này không hợp lệ"
            trimmed.contains("invalid format", ignoreCase = true) ->
                return "Sai định dạng"
            trimmed.contains("required", ignoreCase = true) && !trimmed.contains("scheduledAt", ignoreCase = true) ->
                return "Trường này là bắt buộc"
            trimmed.contains("must not be empty", ignoreCase = true) || trimmed.contains("blank", ignoreCase = true) ->
                return "Trường này không được để trống"
            trimmed.contains("password criteria", ignoreCase = true) || trimmed.contains("password requirements", ignoreCase = true) ->
                return "Mật khẩu phải bao gồm cả chữ, số và ký tự đặc biệt"
            trimmed.contains("invalid email", ignoreCase = true) ->
                return "Định dạng email không hợp lệ"
            trimmed.contains("invalid date format", ignoreCase = true) ->
                return "Sai định dạng ngày tháng"
            trimmed.contains("yyyy-MM-dd HH:mm:ss", ignoreCase = true) ->
                return "Ngày giờ phải theo định dạng yyyy-MM-dd HH:mm:ss"
            trimmed.contains("future date", ignoreCase = true) ->
                return "Ngày phải lớn hơn ngày hiện tại và theo định dạng yyyy-MM-dd"
            trimmed.contains("past date", ignoreCase = true) ->
                return "Ngày sinh phải là ngày trong quá khứ và theo định dạng yyyy-MM-dd"
            trimmed.contains("image format", ignoreCase = true) || trimmed.contains("image type", ignoreCase = true) ->
                return "Chỉ cho phép hình ảnh PNG, JPG, JPEG, WEBP hoặc GIF"
        }

        // 2. Xác thực và Tài khoản (Auth & Register)
        when {
            trimmed.contains("bad credentials", ignoreCase = true) || trimmed.contains("invalid username or password", ignoreCase = true) || trimmed.contains("wrong password", ignoreCase = true) ->
                return "Tên đăng nhập hoặc mật khẩu không chính xác"
            trimmed.contains("old password incorrect", ignoreCase = true) ->
                return "Mật khẩu cũ không chính xác hoặc mật khẩu mới không khớp"
            trimmed.contains("confirm password mismatch", ignoreCase = true) ->
                return "Mật khẩu xác nhận không khớp!"
            trimmed.contains("invalid refresh token", ignoreCase = true) || trimmed.contains("refresh token expired", ignoreCase = true) ->
                return "Refresh token không hợp lệ hoặc đã hết hạn"
            trimmed.contains("email already exists", ignoreCase = true) || trimmed.contains("email is already in use", ignoreCase = true) ->
                return "Email này đã được sử dụng!"
            trimmed.contains("already verified", ignoreCase = true) ->
                return "Tài khoản này đã được xác thực trước đó!"
            trimmed.contains("account not verified", ignoreCase = true) || trimmed.contains("unverified account", ignoreCase = true) ->
                return "Tài khoản chưa được xác thực. Vui lòng kiểm tra email để kích hoạt!"
            trimmed.contains("activate before password reset", ignoreCase = true) ->
                return "Chỉ tài khoản đã kích hoạt mới có thể khôi phục mật khẩu. Vui lòng kiểm tra email xác thực trước!"
            trimmed.contains("invalid otp", ignoreCase = true) || trimmed.contains("otp expired", ignoreCase = true) ->
                return "Mã OTP không chính xác hoặc đã hết hạn"
            trimmed.contains("too fast", ignoreCase = true) || trimmed.contains("wait 60 seconds", ignoreCase = true) || trimmed.contains("rate limit", ignoreCase = true) ->
                return "Bạn đang thao tác quá nhanh, vui lòng đợi 60 giây!"
            trimmed.contains("mail service error", ignoreCase = true) || trimmed.contains("failed to send mail", ignoreCase = true) ->
                return "Hệ thống gửi mail đang gặp sự cố, vui lòng thử lại sau!"
            trimmed.contains("password reset session expired", ignoreCase = true) ->
                return "Phiên đổi mật khẩu đã hết hạn, vui lòng yêu cầu lại OTP"
            trimmed.contains("registration session expired", ignoreCase = true) ->
                return "Phiên đăng ký đã hết hạn hoặc email không tồn tại. Vui lòng đăng ký lại."
            trimmed.contains("system processing error", ignoreCase = true) ->
                return "Lỗi hệ thống khi xử lý dữ liệu. Vui lòng thử lại sau."
        }

        // 3. Đăng nhập bằng Google (Google Auth)
        when {
            trimmed.contains("firebase token", ignoreCase = true) ->
                return "Token Firebase không hợp lệ hoặc đã hết hạn"
            trimmed.contains("google login failed", ignoreCase = true) ->
                return "Đăng nhập Google thất bại. Vui lòng thử lại"
            trimmed.contains("incomplete profile", ignoreCase = true) ->
                return "Vui lòng hoàn thiện thông tin cá nhân trước khi sử dụng"
            trimmed.contains("profile already completed", ignoreCase = true) ->
                return "Hồ sơ đã được hoàn thiện rồi"
            trimmed.contains("google password change", ignoreCase = true) || trimmed.contains("google account change password", ignoreCase = true) ->
                return "Tài khoản đăng nhập bằng Google không hỗ trợ thay đổi mật khẩu"
        }

        // 4. Nhắn tin và Cuộc trò chuyện (Chat)
        when {
            trimmed.contains("conversation not found", ignoreCase = true) || trimmed.contains("chat not found", ignoreCase = true) ->
                return "Không tìm thấy cuộc trò chuyện"
            trimmed.contains("message not found", ignoreCase = true) ->
                return "Không tìm thấy tin nhắn"
            trimmed.contains("target conversation not found", ignoreCase = true) ->
                return "Không tìm thấy cuộc hội thoại đích"
            trimmed.contains("not member of conversation", ignoreCase = true) ->
                return "Bạn không phải là thành viên của cuộc trò chuyện này"
            trimmed.contains("not message sender", ignoreCase = true) ->
                return "Bạn không phải là người gửi tin nhắn này"
            trimmed.contains("max group members", ignoreCase = true) || trimmed.contains("group limit", ignoreCase = true) ->
                return "Nhóm đã đạt giới hạn số lượng thành viên tối đa"
            trimmed.contains("left group", ignoreCase = true) ->
                return "Bạn đã rời khỏi nhóm này"
            trimmed.contains("only group leader", ignoreCase = true) || trimmed.contains("leader action", ignoreCase = true) ->
                return "Chỉ trưởng nhóm mới được thực hiện hành động này"
            trimmed.contains("cannot dissolve 1-1", ignoreCase = true) ->
                return "Không thể giải tán cuộc hội thoại 1-1"
            trimmed.contains("cannot transfer leader to self", ignoreCase = true) ->
                return "Không thể chuyển quyền trưởng nhóm cho chính mình"
            trimmed.contains("user blocked", ignoreCase = true) ->
                return "Người dùng này đã bị chặn"
            trimmed.contains("already blocked", ignoreCase = true) ->
                return "Bạn đã chặn người dùng này trước đó"
            trimmed.contains("cannot block self", ignoreCase = true) ->
                return "Bạn không thể tự chặn chính mình"
            trimmed.contains("recall after 30 minutes", ignoreCase = true) || trimmed.contains("ERR_COMPLETE_TOO_EARLY", ignoreCase = true) ->
                return "Không thể thu hồi tin nhắn sau 30 phút"
            trimmed.contains("no permission to recall", ignoreCase = true) ->
                return "Bạn không có quyền thu hồi tin nhắn này"
            trimmed.contains("recall time expired", ignoreCase = true) ->
                return "Đã quá thời gian cho phép thu hồi tin nhắn"
            trimmed.contains("message already recalled", ignoreCase = true) ->
                return "Tin nhắn này đã được thu hồi trước đó"
            trimmed.contains("already pinned", ignoreCase = true) ->
                return "Tin nhắn này đã được ghim rồi"
            trimmed.contains("not pinned", ignoreCase = true) ->
                return "Tin nhắn này chưa được ghim"
            trimmed.contains("cannot forward recalled", ignoreCase = true) ->
                return "Không thể chuyển tiếp tin nhắn đã thu hồi"
            trimmed.contains("cannot reply recalled", ignoreCase = true) ->
                return "Không thể trả lời tin nhắn đã thu hồi"
            trimmed.contains("cannot report self", ignoreCase = true) ->
                return "Bạn không thể tự báo cáo chính mình"
            trimmed.contains("cannot chat self", ignoreCase = true) ->
                return "Không thể tạo cuộc trò chuyện với chính mình"
            trimmed.contains("invalid file", ignoreCase = true) ->
                return "Tập tin không hợp lệ"
            trimmed.contains("file size limit", ignoreCase = true) ->
                return "Tập tin vượt quá kích thước cho phép"
        }

        // 5. Kết bạn (Friendship)
        when {
            trimmed.contains("cannot friend self", ignoreCase = true) || trimmed.contains("cannot send request to self", ignoreCase = true) ->
                return "Bạn không thể tự gửi lời mời kết bạn cho chính mình"
            trimmed.contains("already friends", ignoreCase = true) ->
                return "Bạn đã là bạn bè với người này rồi"
            trimmed.contains("not friends", ignoreCase = true) ->
                return "Bạn không phải là bạn bè với người này"
            trimmed.contains("friend request already sent", ignoreCase = true) ->
                return "Lời mời kết bạn đã được gửi trước đó"
            trimmed.contains("friend request not found", ignoreCase = true) ->
                return "Không tìm thấy lời mời kết bạn"
            trimmed.contains("not request recipient", ignoreCase = true) ->
                return "Bạn không phải là người nhận lời mời kết bạn này"
            trimmed.contains("user not found by phone", ignoreCase = true) ->
                return "Không tìm thấy người dùng với số điện thoại này"
            trimmed.contains("user not found by keyword", ignoreCase = true) ->
                return "Không tìm thấy người dùng với từ khóa này"
        }

        // 6. Đặt lịch (Booking / Schedule)
        when {
            trimmed.contains("only 1-1 booking", ignoreCase = true) ->
                return "Hiện tại chỉ hỗ trợ đặt lịch hẹn trong cuộc trò chuyện 1-1"
            trimmed.contains("only 1-1 group booking", ignoreCase = true) ->
                return "Chỉ có thể tạo lịch hẹn trong nhóm chat 1-1"
            trimmed.contains("max booking limit (3)", ignoreCase = true) || trimmed.contains("exceeded max booking", ignoreCase = true) ->
                return "Bạn đã vượt quá số lượng đặt lịch hẹn tối đa (3) trong cuộc trò chuyện này."
            trimmed.contains("active booking limit", ignoreCase = true) ->
                return "Bạn đã đạt giới hạn số lượng lịch hẹn đang hoạt động"
            trimmed.contains("partner not found", ignoreCase = true) ->
                return "Không tìm thấy đối tác trong cuộc trò chuyện"
            trimmed.contains("not booking participant", ignoreCase = true) ->
                return "Bạn không phải là người tham gia lịch hẹn này"
            trimmed.contains("booking not found", ignoreCase = true) ->
                return "Không tìm thấy lịch hẹn"
            trimmed.contains("not pending status", ignoreCase = true) || trimmed.contains("not in pending", ignoreCase = true) ->
                return "Lịch hẹn không ở trạng thái PENDING"
            trimmed.contains("invalid booking status", ignoreCase = true) ->
                return "Trạng thái lịch hẹn không hợp lệ cho thao tác này"
            trimmed.contains("no specific time", ignoreCase = true) ->
                return "Lịch hẹn này chưa có thời gian diễn ra cụ thể"
            trimmed.contains("scheduledAt must be future", ignoreCase = true) ->
                return "Thời gian hẹn phải ở trong tương lai"
            trimmed.contains("too soon", ignoreCase = true) ->
                return "Lịch hẹn quá gấp, vui lòng đặt xa hơn"
            trimmed.contains("too late to cancel", ignoreCase = true) ->
                return "Đã quá trễ để hủy lịch hẹn này"
            trimmed.contains("cancel reason required", ignoreCase = true) ->
                return "Vui lòng nhập lý do hủy lịch"
            trimmed.contains("cannot complete before time", ignoreCase = true) ->
                return "Không thể hoàn thành trước giờ hẹn"
            trimmed.contains("only rate partner", ignoreCase = true) ->
                return "Bạn chỉ có thể đánh giá đối tác của mình"
            trimmed.contains("already rated", ignoreCase = true) ->
                return "Bạn đã đánh giá lịch hẹn này rồi"
            trimmed.contains("rating 1 to 5", ignoreCase = true) ->
                return "Điểm đánh giá phải từ 1 đến 5 sao"
            trimmed.contains("location required", ignoreCase = true) ->
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
            trimmed.contains("booking data conversion error", ignoreCase = true) ->
                return "Lỗi khi chuyển đổi dữ liệu đặt lịch hẹn."
            trimmed.contains("cannot book blocked user", ignoreCase = true) ->
                return "Không thể hẹn với người dùng đã bị chặn"
        }

        // 7. Tải lên tập tin (File)
        when {
            trimmed.contains("10MB", ignoreCase = true) || trimmed.contains("file exceeds limit", ignoreCase = true) ->
                return "Kích thước file vượt quá giới hạn 10MB"
            trimmed.contains("empty file", ignoreCase = true) || trimmed.contains("file is empty", ignoreCase = true) ->
                return "File rỗng"
            trimmed.contains("unsupported file format", ignoreCase = true) ->
                return "Định dạng file không được hỗ trợ"
            trimmed.contains("invalid filename", ignoreCase = true) ->
                return "Tên file không hợp lệ"
            trimmed.contains("file upload failed", ignoreCase = true) ->
                return "Tải lên file thất bại"
        }

        // 8. Vị trí (Location)
        when {
            trimmed.contains("user status inactive", ignoreCase = true) ->
                return "Trạng thái người dùng không hoạt động"
            trimmed.contains("enable ready mode", ignoreCase = true) || trimmed.contains("buddy active", ignoreCase = true) ->
                return "Bạn cần bật chế độ Sẵn sàng để cập nhật vị trí"
            trimmed.contains("invalid scan radius", ignoreCase = true) ->
                return "Bán kính quét không hợp lệ"
            trimmed.contains("invalid coordinates", ignoreCase = true) ->
                return "Tọa độ không hợp lệ"
        }

        // 9. Thông báo (Notification)
        when {
            trimmed.contains("notification not found", ignoreCase = true) ->
                return "Không tìm thấy thông báo"
            trimmed.contains("not notification recipient", ignoreCase = true) ->
                return "Bạn không phải là người nhận thông báo này"
            trimmed.contains("notification ids empty", ignoreCase = true) ->
                return "Danh sách ID thông báo không được rỗng"
        }

        // 10. Tìm kiếm User / Quyền User
        when {
            trimmed.startsWith("User not found with username:", ignoreCase = true) || trimmed.contains("not found with username", ignoreCase = true) -> {
                val username = trimmed.substringAfter("username:").trim()
                return "Không tìm thấy người dùng nào với tên đăng nhập: $username"
            }
            trimmed.startsWith("User not found with id:", ignoreCase = true) || trimmed.contains("not found with id", ignoreCase = true) -> {
                val id = trimmed.substringAfter("id:").trim()
                return "Không tìm thấy người dùng nào với id: $id"
            }
            trimmed.contains("no USER role", ignoreCase = true) || trimmed.contains("user role required", ignoreCase = true) ->
                return "Chưa có quyền USER trong hệ thống!"
        }

        // Lỗi mạng & Exception
        if (trimmed.contains("Unable to resolve host", ignoreCase = true) ||
            trimmed.contains("Failed to connect", ignoreCase = true) ||
            trimmed.contains("timeout", ignoreCase = true) ||
            trimmed.contains("Connection refused", ignoreCase = true)
        ) {
            return "Không thể kết nối tới server. Vui lòng kiểm tra lại kết nối mạng!"
        }

        // Nếu thông báo đã là tiếng Việt thì giữ nguyên
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
