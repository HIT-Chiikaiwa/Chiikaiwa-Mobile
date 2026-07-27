package com.example.myapplication.data.remote.websocket

class StompFrameParser {

    data class Frame(
        val command: String,
        val headers: Map<String, String>,
        val body: String
    )

    fun parse(text: String): Frame? {
        val cleanText = text.replace("\r", "")
        val lines = cleanText.split("\n")
        if (lines.isEmpty() || lines[0].isBlank()) return null

        val command = lines[0].trim()
        val headers = mutableMapOf<String, String>()
        var bodyIndex = -1

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isEmpty()) {
                bodyIndex = i + 1
                break
            }
            val colonIdx = line.indexOf(':')
            if (colonIdx != -1) {
                val key = line.substring(0, colonIdx).trim()
                val value = line.substring(colonIdx + 1).trim()
                headers[key] = value
            }
        }

        val body = if (bodyIndex != -1 && bodyIndex < lines.size) {
            lines.subList(bodyIndex, lines.size)
                .joinToString("\n")
                .replace("\u0000", "")
                .trim()
        } else {
            ""
        }

        return Frame(command, headers, body)
    }

    fun buildConnectFrame(token: String): String {
        return "CONNECT\n" +
                "accept-version:1.1,1.2\n" +
                "heart-beat:10000,10000\n" +
                "Authorization:Bearer $token\n\n" +
                "\u0000"
    }

    fun buildSubscribeFrame(destination: String): String {
        return "SUBSCRIBE\n" +
                "id:sub-$destination\n" +
                "destination:$destination\n\n" +
                "\u0000"
    }

    fun buildSendFrame(destination: String, body: String): String {
        return "SEND\n" +
                "destination:$destination\n" +
                "content-type:application/json\n\n" +
                "$body\u0000"
    }

    fun buildDisconnectFrame(): String {
        return "DISCONNECT\n\n\u0000"
    }
}
