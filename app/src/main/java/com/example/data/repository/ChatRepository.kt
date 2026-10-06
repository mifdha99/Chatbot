} catch (e: HttpException) {
            val responseCode = e.response()?.raw()?.code ?: -1

            val friendlyMsg = when (responseCode) {
                400, 401, 403 ->
                    "Hmm, sepertinya Gemini API Key yang kamu masukkan belum tepat atau tidak aktif. Coba cek lagi di menu Pengaturan ya sayang 💕"

                429 ->
                    "Aku lagi agak kewalahan karena batas kuota API tercapai sebentar 🥺 Tunggu beberapa detik lalu coba chat aku lagi ya sayang."

                else ->
                    "Maaf ya sayang, server lagi agak sibuk (Kode $responseCode). Coba sapa aku lagi sebentar lagi ya 💕"
            }

            SendMessageResult.FriendlyError(friendlyMsg)
