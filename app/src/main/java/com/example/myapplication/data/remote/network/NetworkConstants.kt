package com.example.myapplication.data.remote.network

object NetworkConstants {
    const val BASE_HOST = "api.chiikaiwa.me"
    const val BASE_URL = "https://$BASE_HOST/"
    const val WS_URL = "wss://$BASE_HOST/ws/websocket"

    const val AWS_REGION = "ap-southeast-1"
    const val AWS_MAP_API_KEY = "v1.public.eyJqdGkiOiJjOWYzNWZmZi0xYjJhLTRmYjgtYWQ5MC0yMGExNWNmOWUwZjcifak3XT7lozxY9AYaWyNSVfwGyK4iNdbh8MOr-_jGLYieBXLjUh4mqRY8mz1cXHqSyrnOukrPX11SHQm-B4DM4SFpgaaNrv5lVFU8WVcLoPL7RJxnNbXsIygqmcKCJWdA540VdFtQ-uHHeu_KaVMgpVm5fM0XJ6g-f5FGAC80vh1Pk7NjU4R2uoQd1lwd0mtGaV3ZCLb24Jhhr_vGVQJzuj7gJtkwMnSyZaiw4AlHDbr7fzZhh58NlVxBNw1if7RGKra-CBGRA_D1k8jyNpQXJoM9dRefdcEujTWtBuH5R1gtHVTQkLGCxxVFNlNvY4GklQ4Tq-IK84Aso19KQ7VEVeQ.MzRjYzZmZGUtZmY3NC00NDZiLWJiMTktNTc4YjUxYTFlOGZi"
    const val AWS_MAP_STYLE = "Standard"
    const val AWS_MAP_COLOR_SCHEME = "Light"
    const val AWS_MAP_STYLE_URL =
        "https://maps.geo.$AWS_REGION.amazonaws.com/v2/styles/$AWS_MAP_STYLE/descriptor" +
        "?key=$AWS_MAP_API_KEY&color-scheme=$AWS_MAP_COLOR_SCHEME"
}
