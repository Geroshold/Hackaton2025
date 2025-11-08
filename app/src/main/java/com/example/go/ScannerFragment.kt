package com.example.go

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.go.data.JsonPlaceManager
import com.example.go.data.Place
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalGetImage::class)
class ScannerFragment : Fragment() {

    private lateinit var previewView: androidx.camera.view.PreviewView
    private lateinit var resultTextView: TextView
    private lateinit var scanButton: Button
    private lateinit var placeImageView: ImageView

    private lateinit var barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner
    private lateinit var jsonPlaceManager: JsonPlaceManager
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    private var isScanning = false
    private val scannedPlaces = mutableSetOf<String>() // Храним ID уже отсканированных мест
    private var lastScanTime: Long = 0
    private val SCAN_COOLDOWN = 2000L // 2 секунды между сканированиями

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scanner, container, false)

        // Инициализируем views
        previewView = view.findViewById(R.id.preview_view)
        resultTextView = view.findViewById(R.id.result_text)
        scanButton = view.findViewById(R.id.scan_button)
        placeImageView = view.findViewById(R.id.place_image)

        // Инициализируем менеджер JSON данных
        jsonPlaceManager = JsonPlaceManager(requireContext())

        // Загружаем историю посещений при создании
        loadVisitHistory()

        return view
    }

    private fun loadVisitHistory() {
        val sharedPref = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        val history = sharedPref.getStringSet("visit_history", mutableSetOf()) ?: mutableSetOf()

        // Извлекаем ID мест из истории
        scannedPlaces.clear()
        history.forEach { record ->
            val parts = record.split(":")
            if (parts.size >= 3) {
                scannedPlaces.add(parts[2]) // parts[2] это ID места
            }
        }

        Log.d("ScannerFragment", "Загружено ${scannedPlaces.size} посещенных мест из истории")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Сначала скрываем изображение
        placeImageView.visibility = View.GONE

        setupBarcodeScanner()
        setupClickListeners()

        // Предзагружаем данные при создании
        lifecycleScope.launch {
            try {
                val placesCount = jsonPlaceManager.getPlacesCount()
                Log.d("ScannerFragment", "Загружено мест Омска: $placesCount")
            } catch (e: Exception) {
                Log.e("ScannerFragment", "Ошибка загрузки мест: ${e.message}")
            }
        }
    }

    private fun setupBarcodeScanner() {
        barcodeScanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient()
    }

    private fun setupClickListeners() {
        scanButton.setOnClickListener {
            if (hasCameraPermission()) {
                if (isScanning) {
                    stopCamera()
                } else {
                    startCamera()
                }
            } else {
                requestCameraPermission()
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Для сканирования QR-кодов нужен доступ к камере", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Image analysis
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                if (isScanning) {
                    processImageProxy(imageProxy)
                } else {
                    imageProxy.close()
                }
            }

            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                isScanning = true
                scanButton.text = "Остановить сканирование"
                resultTextView.text = "Наведите камеру на QR-код места в Омске"
                placeImageView.visibility = View.GONE

                Log.d("ScannerFragment", "Камера запущена")

            } catch(exc: Exception) {
                Log.e("ScannerFragment", "Use case binding failed", exc)
                Toast.makeText(requireContext(), "Ошибка запуска камеры", Toast.LENGTH_LONG).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun stopCamera() {
        isScanning = false
        scanButton.text = "Начать сканирование"
        Log.d("ScannerFragment", "Камера остановлена")
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = com.google.mlkit.vision.common.InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { rawValue ->
                            activity?.runOnUiThread {
                                processScannedCode(rawValue)
                            }
                        }
                        break // Обрабатываем только первый распознанный код
                    }
                }
                .addOnFailureListener {
                    Log.e("ScannerFragment", "Barcode scanning failed", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun processScannedCode(scannedText: String) {
        // Проверяем cooldown (чтобы избежать множественных сканирований одного кода)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScanTime < SCAN_COOLDOWN) {
            Log.d("ScannerFragment", "Cooldown активен, пропускаем сканирование")
            return
        }

        Log.d("ScannerFragment", "Начало обработки QR-кода: $scannedText")

        lifecycleScope.launch {
            try {
                activity?.runOnUiThread {
                    placeImageView.visibility = View.GONE
                    resultTextView.text = "Обработка QR-кода..."
                }

                // Обрабатываем отсканированный код
                processScannedContent(scannedText)

            } catch (e: Exception) {
                activity?.runOnUiThread {
                    resultTextView.text = "Ошибка при обработке QR-кода: ${e.message}"
                    Log.e("ScannerFragment", "Ошибка в processScannedCode: ${e.message}", e)
                }
            }
        }
    }

    private suspend fun processScannedContent(scannedText: String) {
        Log.d("ScannerFragment", "Сканированный текст: $scannedText")

        // Извлекаем ID места из сканированного текста
        val placeId = jsonPlaceManager.extractPlaceIdFromUrl(scannedText)

        if (placeId != null) {
            // Проверяем, не сканировали ли мы уже это место
            if (scannedPlaces.contains(placeId)) {
                // Используем корутину для вызова suspend функции
                val place = jsonPlaceManager.getPlaceById(placeId)
                activity?.runOnUiThread {
                    val placeName = place?.name ?: "это место"
                    Toast.makeText(requireContext(), "❌ Вы уже посещали $placeName!", Toast.LENGTH_LONG).show()
                    resultTextView.text = """
                        ❌ ВЫ УЖЕ ПОСЕЩАЛИ ЭТО МЕСТО!
                        
                        🏛️ ${place?.name ?: "Неизвестное место"}
                        📍 ${place?.location ?: "Адрес не указан"}
                        💰 Баллов за посещение: ${place?.points ?: 0}
                        
                        Вы не можете получить баллы за повторное посещение одного и того же места.
                        
                        Найдите другое место для посещения!
                    """.trimIndent()
                    stopCamera()
                }
                return
            }

            Log.d("ScannerFragment", "Извлеченный ID места: $placeId")

            // Ищем место по ID (это suspend функция, вызываем напрямую)
            val place = jsonPlaceManager.getPlaceById(placeId)
            if (place != null) {
                Log.d("ScannerFragment", "Место найдено: ${place.name}")
                lastScanTime = System.currentTimeMillis()

                // Добавляем место в список посещенных
                scannedPlaces.add(placeId)

                // Сначала показываем информацию, потом останавливаем камеру
                showPlaceInfo(place)
                saveVisitToProfile(place)
                jsonPlaceManager.incrementVisitorCount(placeId)

                // Останавливаем сканирование после успешного распознавания
                activity?.runOnUiThread {
                    stopCamera()
                    // Обновляем текст кнопки
                    scanButton.text = "Сканировать снова"
                }
            } else {
                Log.d("ScannerFragment", "Место не найдено по ID: $placeId")
                showPlaceNotFoundError(placeId)
            }
        } else {
            Log.d("ScannerFragment", "Не удалось извлечь ID из: $scannedText")
            showInvalidQrError(scannedText)
        }
    }

    private fun showPlaceInfo(place: Place) {
        val currentTime = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val totalPoints = getUserTotalPoints()

        val resultText = """
            ✅ МЕСТО РАСПОЗНАНО!
            
            🏛️ ${place.name}
            📍 ${place.location}
            💰 Начислено баллов: ${place.points}
            👥 Всего посетителей: ${place.visitors + 1}
            📅 Время посещения: $currentTime
            
            📝 ОПИСАНИЕ:
            ${place.description.replace("\\n", "\n")}
            
            💎 Ваши общие баллы: $totalPoints
            🎉 Поздравляем с посещением! Место добавлено в вашу коллекцию.
        """.trimIndent()

        activity?.runOnUiThread {
            resultTextView.text = resultText

            // Загружаем и показываем изображение
            if (place.imageUrl.isNotEmpty()) {
                loadPlaceImage(place.imageUrl)
            } else {
                placeImageView.visibility = View.GONE
            }

            Log.d("ScannerFragment", "Информация о месте показана: ${place.name}")
        }
    }

    private fun getUserTotalPoints(): Int {
        val sharedPref = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        return sharedPref.getInt("user_points", 0)
    }

    private fun loadPlaceImage(imageUrl: String) {
        try {
            placeImageView.visibility = View.VISIBLE

            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                // Загрузка из интернета
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(placeImageView)
                Log.d("ScannerFragment", "Изображение загружается из: $imageUrl")
            } else {
                // Локальное изображение
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(placeImageView)
                Log.d("ScannerFragment", "Локальное изображение загружается: $imageUrl")
            }
        } catch (e: Exception) {
            Log.e("ScannerFragment", "Ошибка загрузки изображения: ${e.message}")
            placeImageView.visibility = View.GONE
        }
    }

    private fun showPlaceNotFoundError(placeId: String) {
        activity?.runOnUiThread {
            val errorText = """
                ❌ МЕСТО НЕ НАЙДЕНО
                
                ID: $placeId
                
                Данное место не найдено в базе данных Омска.
                Проверьте корректность QR-кода.
                
                Убедитесь, что QR-код содержит правильный идентификатор места.
            """.trimIndent()

            resultTextView.text = errorText
            placeImageView.visibility = View.GONE
        }
    }

    private fun showInvalidQrError(scannedText: String) {
        activity?.runOnUiThread {
            val errorText = """
                ❌ НЕРАСПОЗНАННЫЙ QR-КОД
                
                Содержимое: $scannedText
                
                Это не QR-код места из Омска.
                Отсканируйте правильный QR-код.
                
                Поддерживаемые форматы:
                • yourapp://place/ID_МЕСТА
                • Прямой ID места
            """.trimIndent()

            resultTextView.text = errorText
            placeImageView.visibility = View.GONE
        }
    }

    private fun saveVisitToProfile(place: Place) {
        val sharedPref = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)

        // Сохраняем историю посещений с баллами
        val history = sharedPref.getStringSet("visit_history", mutableSetOf()) ?: mutableSetOf()
        val visitRecord = "${System.currentTimeMillis()}:${place.name}:${place.id}:${place.points}"
        val newHistory = mutableSetOf<String>()
        newHistory.addAll(history)
        newHistory.add(visitRecord)

        sharedPref.edit().putStringSet("visit_history", newHistory).apply()

        // Сохраняем общее количество посещений
        val totalVisits = sharedPref.getInt("total_visits", 0) + 1
        sharedPref.edit().putInt("total_visits", totalVisits).apply()

        // Сохраняем общее количество баллов
        val totalPoints = sharedPref.getInt("user_points", 0) + place.points
        sharedPref.edit().putInt("user_points", totalPoints).apply()

        // Сохраняем последнее посещенное место
        sharedPref.edit().putString("last_visited_place", place.name).apply()
        sharedPref.edit().putLong("last_visit_time", System.currentTimeMillis()).apply()
        sharedPref.edit().putInt("last_visit_points", place.points).apply()

        activity?.runOnUiThread {
            Toast.makeText(
                requireContext(),
                "✅ +${place.points} баллов за: ${place.name}!\nВсего баллов: $totalPoints",
                Toast.LENGTH_LONG
            ).show()
        }

        Log.d("ScannerFragment", "Посещение сохранено: ${place.name}, +${place.points} баллов, всего: $totalPoints")
    }

    override fun onResume() {
        super.onResume()
        // Загружаем историю при каждом возобновлении фрагмента
        loadVisitHistory()

        val totalPoints = getUserTotalPoints()
        if (!isScanning) {
            resultTextView.text = """
                Нажмите кнопку для начала сканирования
                
                📊 Статистика:
                • Посещено мест: ${scannedPlaces.size}
                • Ваши баллы: $totalPoints
                
                Сканируйте QR-коды мест Омска и получайте баллы за посещение!
            """.trimIndent()
        }
    }

    override fun onPause() {
        super.onPause()
        stopCamera()
        cameraExecutor.shutdown()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCamera()
        cameraExecutor.shutdown()
    }
}