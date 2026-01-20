package com.duy.wwmupdater

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_OPEN_DIR = 100
    private lateinit var txtStatus: TextView
    
    // URL file test (Logo Google nhỏ gọn)
    private val TEST_FILE_URL = "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png"
    // Tên file sẽ lưu vào thư mục game
    private val TARGET_FILE_NAME = "test_download_image.png"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnAction = findViewById<Button>(R.id.btnUpdate)
        txtStatus = findViewById(R.id.txtStatus) // Bạn nhớ thêm TextView có id txtStatus vào file XML nhé (hoặc xóa dòng này nếu lười)

        btnAction.setOnClickListener {
            // Bước 1: Yêu cầu chọn thư mục
            openDirectoryPicker()
        }
    }

    private fun openDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }
        startActivityForResult(intent, REQUEST_CODE_OPEN_DIR)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_DIR && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                
                // Bắt đầu quy trình test download
                runTestDownloadProcess(uri)
            }
        }
    }

    private fun runTestDownloadProcess(rootUri: Uri) {
        // Sử dụng Coroutine để chạy ngầm (tránh đơ app)
        lifecycleScope.launch(Dispatchers.IO) {
            
            // 1. Cập nhật giao diện: Đang xử lý
            withContext(Dispatchers.Main) {
                txtStatus.text = "Đang tìm đường dẫn..."
                Toast.makeText(this@MainActivity, "Đang xử lý...", Toast.LENGTH_SHORT).show()
            }

            // 2. Tạo đường dẫn thư mục đích
            val rootDir = DocumentFile.fromTreeUri(this@MainActivity, rootUri)
            // Lưu vào thư mục test thử: files/LocalData/Patch/HD/oversea/locale
            val targetFolder = createPath(rootDir, "files/LocalData/Patch/HD/oversea/locale")

            if (targetFolder != null) {
                withContext(Dispatchers.Main) { txtStatus.text = "Đang tải file từ web..." }
                
                // 3. Gọi hàm tải và ghi file
                val success = downloadAndSaveFile(TEST_FILE_URL, targetFolder, TARGET_FILE_NAME)

                // 4. Thông báo kết quả
                withContext(Dispatchers.Main) {
                    if (success) {
                        txtStatus.text = "Thành công! Kiểm tra file $TARGET_FILE_NAME"
                        Toast.makeText(this@MainActivity, "Đã tải và chép file thành công!", Toast.LENGTH_LONG).show()
                    } else {
                        txtStatus.text = "Thất bại! Xem Logcat để biết lỗi."
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    txtStatus.text = "Lỗi: Không tạo được thư mục game."
                }
            }
        }
    }

    // Hàm quan trọng: Vừa tải vừa ghi (Stream)
    private fun downloadAndSaveFile(urlStr: String, folder: DocumentFile, fileName: String): Boolean {
        return try {
            // Xóa file cũ nếu có
            folder.findFile(fileName)?.delete()
            
            // Tạo file mới
            val newFile = folder.createFile("image/png", fileName) ?: return false
            
            // Mở kết nối mạng
            val url = URL(urlStr)
            val connection = url.openConnection()
            connection.connect()

            // Stream đọc từ mạng (Input)
            val inputStream = connection.getInputStream()
            
            // Stream ghi vào thư mục game (Output)
            val outputStream = contentResolver.openOutputStream(newFile.uri)

            if (outputStream != null) {
                // Copy dữ liệu từ mạng thẳng vào file
                val buffer = ByteArray(4096) // Buffer 4KB
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                
                // Đóng stream
                outputStream.flush()
                outputStream.close()
                inputStream.close()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("WWMUpdater", "Lỗi tải file: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Hàm tiện ích tạo đường dẫn (giữ nguyên như trước)
    private fun createPath(root: DocumentFile?, path: String): DocumentFile? {
        if (root == null) return null
        val parts = path.split("/")
        var currentDir = root
        for (part in parts) {
            var nextDir = currentDir.findFile(part)
            if (nextDir == null || !nextDir.isDirectory) {
                nextDir = currentDir.createDirectory(part)
            }
            if (nextDir == null) return null
            currentDir = nextDir
        }
        return currentDir
    }
}