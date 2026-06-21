package com.topscore.errornotebook.ui.question

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.topscore.errornotebook.R
import com.topscore.errornotebook.databinding.FragmentAddQuestionBinding
import com.topscore.errornotebook.domain.model.ErrorReason
import com.topscore.errornotebook.domain.model.QuestionType
import com.topscore.errornotebook.domain.model.SubjectStage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Add Question Fragment
 * Step-by-step flow for adding a new question:
 * 1. SELECT_SOURCE - Select image source (camera/album)
 * 2. CAMERA_CAPTURE - Camera capture
 * 3. SELECT_REGION - Select question region (crop)
 * 4. OCR_RECOGNIZING - OCR progress
 * 5. CONFIRM_RESULT - Confirm OCR result
 * 6. FILL_INFO - Fill in question info form
 */
@AndroidEntryPoint
class AddQuestionFragment : Fragment() {

    private var _binding: FragmentAddQuestionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddQuestionViewModel by viewModels()

    // CameraX
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    // Gallery picker
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // Load bitmap from URI and set for cropping
            try {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    viewModel.setImageFromAlbum(it)
                    viewModel.setCapturedBitmap(bitmap)
                } else {
                    Toast.makeText(requireContext(), "无法加载图片", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "图片加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera permission
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        setupViews()
        observeState()
    }

    private fun setupViews() {
        // Back button
        binding.btnBack.setOnClickListener {
            viewModel.goBack()
        }

        // Step 1: Source selection
        binding.cardCamera.setOnClickListener {
            checkCameraPermissionAndCapture()
        }

        binding.cardAlbum.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Step 3: Select Region
        binding.btnCancelRegion.setOnClickListener {
            viewModel.cancelCropRegion()
        }

        binding.btnConfirmRegion.setOnClickListener {
            val cropRect = binding.cropSelectionView.getCropRect()
            if (cropRect != null) {
                viewModel.confirmCropRegion(cropRect)
            } else {
                Toast.makeText(requireContext(), "请先选择区域", Toast.LENGTH_SHORT).show()
            }
        }

        // Step 4: Confirm result
        binding.btnRetake.setOnClickListener {
            viewModel.retake()
        }

        binding.btnUseResult.setOnClickListener {
            viewModel.useOcrResult()
        }

        // Step 5: Form fields
        setupFormFields()

        // Save button
        binding.btnSave.setOnClickListener {
            viewModel.saveQuestion()
        }
    }

    private fun setupFormFields() {
        // Stage spinner
        val stageAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            SubjectStage.entries.map { getStageText(it) }
        )
        binding.spinnerStage.setAdapter(stageAdapter)
        binding.spinnerStage.setOnItemClickListener { _, _, position, _ ->
            viewModel.updateStage(SubjectStage.entries[position])
        }

        // Subject dropdown with common subjects
        val subjects = listOf(
            getString(R.string.subject_math),
            getString(R.string.subject_chinese),
            getString(R.string.subject_english),
            getString(R.string.subject_physics),
            getString(R.string.subject_chemistry),
            getString(R.string.subject_biology),
            getString(R.string.subject_politics),
            getString(R.string.subject_history),
            getString(R.string.subject_geography)
        )
        val subjectAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            subjects
        )
        binding.etSubject.setAdapter(subjectAdapter)
        binding.etSubject.setOnItemClickListener { _, _, position, _ ->
            viewModel.updateSubject(subjects[position])
        }

        // Error reason spinner
        val reasonAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            ErrorReason.entries.map { getErrorReasonText(it) }
        )
        binding.spinnerErrorReason.setAdapter(reasonAdapter)
        binding.spinnerErrorReason.setOnItemClickListener { _, _, position, _ ->
            viewModel.updateErrorReason(ErrorReason.entries[position])
        }

        // Question type spinner
        val typeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            QuestionType.entries.map { getQuestionTypeText(it) }
        )
        binding.spinnerQuestionType.setAdapter(typeAdapter)
        binding.spinnerQuestionType.setOnItemClickListener { _, _, position, _ ->
            viewModel.updateQuestionType(QuestionType.entries[position])
        }

        // Error date picker
        binding.tilErrorDate.setEndIconOnClickListener {
            showDatePicker()
        }
        binding.etErrorDate.setOnClickListener {
            showDatePicker()
        }

        // Text change listeners
        binding.etSubject.doAfterTextChanged { viewModel.updateSubject(it.toString()) }
        binding.etSource.doAfterTextChanged { viewModel.updateSource(it.toString()) }
        binding.etCorrectAnswer.doAfterTextChanged { viewModel.updateCorrectAnswer(it.toString()) }
        binding.etWrongAnswer.doAfterTextChanged { viewModel.updateWrongAnswer(it.toString()) }
        binding.etNotes.doAfterTextChanged { viewModel.updateNotes(it.toString()) }

        // Tags chip group
        binding.chipGroupTags.setOnCheckedStateChangeListener { group, checkedIds ->
            val tags = checkedIds.mapNotNull { id ->
                group.findViewById<Chip>(id)?.text?.toString()
            }
            viewModel.updateTags(tags)
        }
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择错题日期")
            .setSelection(viewModel.uiState.value.errorDate)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            viewModel.updateErrorDate(selection)
            binding.etErrorDate.setText(formatDate(selection))
        }

        datePicker.show(childFragmentManager, "datePicker")
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun checkCameraPermissionAndCapture() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.selectSource(isCamera = true)
            }
            else -> {
                requestPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "相机启动失败", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            requireContext().cacheDir,
            "question_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModel.setImageFromCamera(photoFile.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        requireContext(),
                        "拍照失败: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        updateUi(state)
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun updateUi(state: AddQuestionUiState) {
        // Update step indicator
        updateStepIndicator(state.currentStep)

        // Show/hide step views
        binding.stepSource.isVisible = state.currentStep == AddQuestionStep.SELECT_SOURCE
        binding.stepCamera.isVisible = state.currentStep == AddQuestionStep.CAMERA_CAPTURE
        binding.stepSelectRegion.isVisible = state.currentStep == AddQuestionStep.SELECT_REGION
        binding.stepOcr.isVisible = state.currentStep == AddQuestionStep.OCR_RECOGNIZING
        binding.stepConfirmResult.isVisible = state.currentStep == AddQuestionStep.CONFIRM_RESULT
        binding.stepFillInfo.isVisible = state.currentStep == AddQuestionStep.FILL_INFO

        // Loading overlay
        binding.progressBar.isVisible = state.isLoading

        // Step 2: Camera - start camera when entering this step
        if (state.currentStep == AddQuestionStep.CAMERA_CAPTURE) {
            startCamera()
            binding.btnCapture.setOnClickListener { takePhoto() }
        }

        // Step 3: Select Region - load bitmap into crop view
        if (state.currentStep == AddQuestionStep.SELECT_REGION) {
            // Release camera when entering crop step
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                } catch (e: Exception) {
                    // Ignore
                }
            }, ContextCompat.getMainExecutor(requireContext()))

            // Load bitmap into crop selection view
            state.capturedBitmap?.let { bitmap ->
                binding.cropSelectionView.setImageBitmap(bitmap)
            }
        }

        // Step 4: OCR Recognizing
        if (state.currentStep == AddQuestionStep.OCR_RECOGNIZING) {
            // Camera is no longer needed, release
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                } catch (e: Exception) {
                    // Ignore
                }
            }, ContextCompat.getMainExecutor(requireContext()))
        }

        // Step 5: Confirm result
        if (state.currentStep == AddQuestionStep.CONFIRM_RESULT) {
            state.ocrResult?.let { ocr ->
                binding.tvRecognizedText.text = ocr.text
                binding.tvOcrConfidence.text = "识别置信度: ${(ocr.confidence * 100).toInt()}%"
            }
            binding.tvErrorOcr.text = state.errorMessage ?: ""
            binding.tvErrorOcr.isVisible = state.errorMessage != null
        }

        // Step 6: Fill info - populate form with existing values
        if (state.currentStep == AddQuestionStep.FILL_INFO) {
            populateForm(state)
        }
    }

    private fun updateStepIndicator(step: AddQuestionStep) {
        val steps = listOf(
            binding.stepIndicator1,
            binding.stepIndicator2,
            binding.stepIndicator3,
            binding.stepIndicator4,
            binding.stepIndicator5
        )

        // Map steps to indicator indices
        // SELECT_REGION shares indicator with OCR_RECOGNIZING (both part of "process" phase)
        val stepIndex = when (step) {
            AddQuestionStep.SELECT_SOURCE -> 0
            AddQuestionStep.CAMERA_CAPTURE -> 1
            AddQuestionStep.SELECT_REGION -> 2
            AddQuestionStep.OCR_RECOGNIZING -> 3
            AddQuestionStep.CONFIRM_RESULT -> 4
            AddQuestionStep.FILL_INFO -> 4
        }

        steps.forEachIndexed { index, view ->
            view.isSelected = index <= stepIndex
        }

        // Update step titles
        binding.tvStepTitle.text = when (step) {
            AddQuestionStep.SELECT_SOURCE -> getString(R.string.select_source)
            AddQuestionStep.CAMERA_CAPTURE -> getString(R.string.camera_import)
            AddQuestionStep.SELECT_REGION -> getString(R.string.select_region)
            AddQuestionStep.OCR_RECOGNIZING -> getString(R.string.ocr_recognizing)
            AddQuestionStep.CONFIRM_RESULT -> getString(R.string.confirm_result)
            AddQuestionStep.FILL_INFO -> getString(R.string.fill_info)
        }
    }

    private fun populateForm(state: AddQuestionUiState) {
        // Only update if different to avoid cursor jumping
        if (binding.spinnerStage.text.toString() != getStageText(state.stage)) {
            binding.spinnerStage.setText(getStageText(state.stage), false)
        }

        if (binding.etSubject.text.toString() != state.subject) {
            binding.etSubject.setText(state.subject)
        }

        if (binding.spinnerErrorReason.text.toString() != getErrorReasonText(state.errorReason)) {
            binding.spinnerErrorReason.setText(getErrorReasonText(state.errorReason), false)
        }

        if (binding.etSource.text.toString() != state.source) {
            binding.etSource.setText(state.source)
        }

        if (binding.spinnerQuestionType.text.toString() != getQuestionTypeText(state.questionType)) {
            binding.spinnerQuestionType.setText(getQuestionTypeText(state.questionType), false)
        }

        if (binding.etCorrectAnswer.text.toString() != state.correctAnswer) {
            binding.etCorrectAnswer.setText(state.correctAnswer)
        }

        if (binding.etWrongAnswer.text.toString() != state.wrongAnswer) {
            binding.etWrongAnswer.setText(state.wrongAnswer)
        }

        if (binding.etNotes.text.toString() != state.notes) {
            binding.etNotes.setText(state.notes)
        }

        binding.etErrorDate.setText(formatDate(state.errorDate))

        // Update tags
        binding.chipGroupTags.removeAllViews()
        state.tags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag
                isCheckable = true
                isChecked = true
            }
            binding.chipGroupTags.addView(chip)
        }
    }

    private fun handleEvent(event: AddQuestionEvent) {
        when (event) {
            is AddQuestionEvent.SaveSuccess -> {
                Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_addQuestion_to_questionList)
            }
            is AddQuestionEvent.ShowError -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            }
            is AddQuestionEvent.NavigateBack -> {
                findNavController().popBackStack()
            }
        }
    }

    private fun getStageText(stage: SubjectStage): String {
        return when (stage) {
            SubjectStage.PRIMARY -> getString(R.string.stage_primary)
            SubjectStage.MIDDLE -> getString(R.string.stage_middle)
            SubjectStage.HIGH -> getString(R.string.stage_high)
        }
    }

    private fun getErrorReasonText(reason: ErrorReason): String {
        return when (reason) {
            ErrorReason.MISREAD -> getString(R.string.error_misread)
            ErrorReason.CALC_ERROR -> getString(R.string.error_calc)
            ErrorReason.CONCEPT_UNCLEAR -> getString(R.string.error_concept)
            ErrorReason.KNOWLEDGE_GAP -> getString(R.string.error_knowledge)
            ErrorReason.CARELESS -> getString(R.string.error_careless)
            ErrorReason.OTHER -> getString(R.string.error_other)
        }
    }

    private fun getQuestionTypeText(type: QuestionType): String {
        return when (type) {
            QuestionType.CHOICE -> getString(R.string.type_choice)
            QuestionType.FILL_BLANK -> getString(R.string.type_fill_blank)
            QuestionType.SOLUTION -> getString(R.string.type_solution)
            QuestionType.PROOF -> getString(R.string.type_proof)
            QuestionType.OTHER -> getString(R.string.type_other)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
