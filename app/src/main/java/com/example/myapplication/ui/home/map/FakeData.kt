package com.example.myapplication.ui.home.map

import com.example.myapplication.data.model.response.NearbyUserResponse

object FakeData {
    fun getFakeNearbyUsers(lat: Double, lng: Double): List<NearbyUserResponse> {
        return listOf(
            NearbyUserResponse(
                userId = "fake_1",
                firstName = "An",
                lastName = "Nguyễn Văn",
                avatar = "frame1",
                university = "Đại học Bách Khoa Hà Nội",
                majorName = "Khoa học Máy tính",
                statusTag = "Đang tìm nhóm học giải tích",
                latitude = lat + 0.002,
                longitude = lng + 0.003,
                distanceKm = 0.35
            ),
            NearbyUserResponse(
                userId = "fake_2",
                firstName = "Chi",
                lastName = "Lê Thị",
                avatar = "frame2",
                university = "Đại học Quốc gia Hà Nội",
                majorName = "Công nghệ Thông tin",
                statusTag = "Rảnh chiều nay, ai cafe không?",
                latitude = lat - 0.003,
                longitude = lng + 0.004,
                distanceKm = 0.62
            ),
            NearbyUserResponse(
                userId = "fake_3",
                firstName = "Dũng",
                lastName = "Trần Minh",
                avatar = "frame4",
                university = "Đại học Công nghiệp Hà Nội",
                majorName = "Kỹ thuật Phần mềm",
                statusTag = "Cần tìm bạn học cùng tiếng Anh",
                latitude = lat + 0.004,
                longitude = lng - 0.003,
                distanceKm = 0.71
            ),
            NearbyUserResponse(
                userId = "fake_4",
                firstName = "Hương",
                lastName = "Phạm Mai",
                avatar = "frame1",
                university = "Đại học Ngoại thương",
                majorName = "Kinh tế Quốc tế",
                statusTag = "Muốn trao đổi kiến thức UI/UX",
                latitude = lat - 0.001,
                longitude = lng - 0.002,
                distanceKm = 0.28
            ),
            NearbyUserResponse(
                userId = "fake_5",
                firstName = "Nam",
                lastName = "Hoàng Hải",
                avatar = "frame2",
                university = "Học viện Công nghệ Bưu chính Viễn thông",
                majorName = "An toàn thông tin",
                statusTag = "Cần pro chỉ giáo Web Security",
                latitude = lat + 0.001,
                longitude = lng - 0.005,
                distanceKm = 0.65
            ),
            NearbyUserResponse(
                userId = "fake_6",
                firstName = "Tuấn",
                lastName = "Lê Anh",
                avatar = "frame1",
                university = "Đại học Xây dựng",
                majorName = "Kỹ thuật Xây dựng",
                statusTag = "Tìm bạn chạy bộ ngoài phạm vi 6km",
                latitude = lat + 0.07,
                longitude = lng + 0.07,
                distanceKm = 10.9
            ),
            NearbyUserResponse(
                userId = "fake_7",
                firstName = "Vy",
                lastName = "Nguyễn Thảo",
                avatar = "frame4",
                university = "Đại học Luật Hà Nội",
                majorName = "Luật Kinh tế",
                statusTag = "Rất xa, ở ngoài 10km",
                latitude = lat - 0.08,
                longitude = lng - 0.08,
                distanceKm = 12.5
            )
        )
    }
}
