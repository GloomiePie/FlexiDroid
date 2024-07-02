package com.clipp.devicemanagerapp

import android.Manifest
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.PowerManager
import android.os.PowerManager.WakeLock

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private lateinit var audioManager: AudioManager
    private lateinit var powerManager: PowerManager
    private var wakeLock: WakeLock? = null

    companion object {
        const val REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val btnActivateKiosk = findViewById<Button>(R.id.btnEnableKioskMode)
        btnActivateKiosk.setOnClickListener {
            setKioskMode(true)
        }

        val btnDeactivateKiosk = findViewById<Button>(R.id.btnDisableKioskMode)
        btnDeactivateKiosk.setOnClickListener {
            setKioskMode(false)
        }

        val btnRequestAdmin = findViewById<Button>(R.id.btnRequestAdmin)
        btnRequestAdmin.setOnClickListener {
            requestDeviceAdmin()
        }

        val btnChangeDateTime = findViewById<Button>(R.id.btnChangeDateTime)
        btnChangeDateTime.setOnClickListener {
            showDateTimePicker()
        }

        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, DeviceAdminReceiver::class.java)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.MODIFY_PHONE_STATE,
            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.REQUEST_INSTALL_PACKAGES,
            Manifest.permission.EXPAND_STATUS_BAR
        )

        requestPermissionsIfNecessary(permissions)
        initializeButtons()
    }


    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Permite activar el modo quiosco.")
        startActivityForResult(intent, 1)
    }

    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, permissions, 0)
            }
        }
    }

    private fun initializeButtons() {
        // Botón para reiniciar el dispositivo
        findViewById<Button>(R.id.btnReboot).setOnClickListener { rebootDevice() }

        // Botón para apagar el dispositivo
        findViewById<Button>(R.id.btnShutdown).setOnClickListener { shutdownDevice() }

        // Botones para habilitar y deshabilitar Bluetooth
        findViewById<Button>(R.id.btnEnableBluetooth).setOnClickListener { setBluetooth(true) }
        findViewById<Button>(R.id.btnDisableBluetooth).setOnClickListener { setBluetooth(false) }

        // Botones para habilitar y deshabilitar GPS
        findViewById<Button>(R.id.btnEnableGPS).setOnClickListener { setGPS(true) }
        findViewById<Button>(R.id.btnDisableGPS).setOnClickListener { setGPS(false) }

        // Botones para habilitar y deshabilitar datos móviles
        findViewById<Button>(R.id.btnEnableMobileData).setOnClickListener { setMobileData(true) }
        findViewById<Button>(R.id.btnDisableMobileData).setOnClickListener { setMobileData(false) }

        // Botones para encender y apagar la pantalla
        findViewById<Button>(R.id.btnEnableScreen).setOnClickListener { setScreenOn(true) }
        findViewById<Button>(R.id.btnDisableScreen).setOnClickListener { setScreenOn(false) }

        // Botones para habilitar y deshabilitar modo avión
        // findViewById<Button>(R.id.btnEnableAirplaneMode).setOnClickListener { setAirplaneMode(true) }
        // findViewById<Button>(R.id.btnDisableAirplaneMode).setOnClickListener { setAirplaneMode(false) }

        findViewById<Button>(R.id.btnEnableKioskMode).setOnClickListener { setKioskMode(true) }
        findViewById<Button>(R.id.btnDisableKioskMode).setOnClickListener { setKioskMode(false) }
        findViewById<Button>(R.id.btnAdjustVolume).setOnClickListener { showVolumeDialog() }
        findViewById<Button>(R.id.btnAdjustBrightness).setOnClickListener { showBrightnessDialog() }
        findViewById<Button>(R.id.btnChangeDateTime).setOnClickListener { showDateTimePicker() }
        findViewById<Button>(R.id.btnInstallApp).setOnClickListener { installApp() }
        findViewById<Button>(R.id.btnUninstallApp).setOnClickListener { uninstallApp() }
    }

    // Función para reiniciar el dispositivo
    private fun rebootDevice() {
        if (devicePolicyManager.isAdminActive(compName)) {
            devicePolicyManager.reboot(compName)
            showToast("Reiniciando dispositivo")
        } else {
            showToast("Permiso de administrador de dispositivo requerido para reiniciar")
        }
    }

    // Función para apagar el dispositivo
    private fun shutdownDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            showToast("Este dispositivo no tiene permisos para apagar automáticamente")
        } else {
            val intent = Intent(Intent.ACTION_SHUTDOWN)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    // Función para controlar el Bluetooth
    private fun setBluetooth(enable: Boolean) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_CODE)
            return
        }

        val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        if (enable) {
            bluetoothAdapter.enable()
            showToast("Bluetooth activado")
        } else {
            bluetoothAdapter.disable()
            showToast("Bluetooth desactivado")
        }
    }

    // Función para controlar el GPS
    private fun setGPS(enable: Boolean) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    // Función para controlar los datos móviles
    private fun setMobileData(enable: Boolean) {
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    // Función para encender y apagar la pantalla
    private fun setScreenOn(enable: Boolean) {
        if (enable) {
            val wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "app:myWakeLock"
            )
            wakeLock.acquire(10 * 60 * 1000L /* 10 minutes */)
            showToast("Pantalla encendida")
        } else {
            if (devicePolicyManager.isAdminActive(compName)) {
                devicePolicyManager.lockNow()
                showToast("Pantalla apagada")
            } else {
                showToast("Permiso de administrador de dispositivo requerido para apagar la pantalla")
            }
        }
    }

    // Función para controlar el modo quiosco
    private fun setKioskMode(enable: Boolean) {
        if (enable) {
            startLockTask()
            showToast("Modo quiosco activado")
        } else {
            stopLockTask()
            showToast("Modo quiosco desactivado")
        }
    }

    // Función para mostrar el selector de fecha y hora
    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val timePickerDialog = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        // Aquí puedes actualizar la fecha y hora del sistema si tienes los permisos adecuados
                        // O mostrar la fecha y hora seleccionada en un TextView, por ejemplo
                        val selectedDateTime = findViewById<TextView>(R.id.selectedDateTime)
                        selectedDateTime.text = calendar.time.toString()
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    // Función para cambiar la fecha y hora del sistema
    private fun setDateTime(year: Int, month: Int, day: Int, hourOfDay: Int, minute: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, hourOfDay, minute)
        val timestamp = calendar.timeInMillis

        // Depending on your Android version and permissions, you may need to handle this differently.
        // Example below assumes you have the necessary permissions to change system settings.
        val intent = Intent(Settings.ACTION_DATE_SETTINGS)
        startActivity(intent)
    }

    // Función para instalar una aplicación
    private fun installApp() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://example.com/app.apk")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    // Función para desinstalar una aplicación
    private fun uninstallApp() {
        val packageUri = Uri.parse("package:com.example.someapp")
        val intent = Intent(Intent.ACTION_DELETE, packageUri)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showVolumeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_volume_control, null)
        val seekBarVolume = dialogView.findViewById<SeekBar>(R.id.seekBarVolume)
        val tvVolume = dialogView.findViewById<TextView>(R.id.tvVolume)

        // Set initial progress to current volume
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        seekBarVolume.progress = currentVolume

        // Update volume text view
        tvVolume.text = "Ajustar Volumen: $currentVolume"

        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_PLAY_SOUND)
                tvVolume.text = "Ajustar Volumen: $progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        AlertDialog.Builder(this)
            .setTitle("Control de Volumen")
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showBrightnessDialog() {
        if (Settings.System.canWrite(this)) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_brightness_control, null)
            val seekBarBrightness = dialogView.findViewById<SeekBar>(R.id.seekBarBrightness)
            val tvBrightness = dialogView.findViewById<TextView>(R.id.tvBrightness)

            // Set the maximum progress to 100 (representing 100%)
            seekBarBrightness.max = 100

            // Get the current screen brightness in percentage
            val currentBrightness = getCurrentBrightnessPercent()
            seekBarBrightness.progress = currentBrightness

            // Update brightness text view
            tvBrightness.text = "Ajustar Brillo: $currentBrightness%"

            seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    // Calculate the brightness value from percentage to the range 0-255
                    val brightnessValue = (progress * 255) / 100
                    // Apply the brightness change immediately
                    applyBrightness(brightnessValue)
                    // Update brightness text view
                    tvBrightness.text = "Ajustar Brillo: $progress%"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })

            AlertDialog.Builder(this)
                .setTitle("Control de Brillo")
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show()
        } else {
            // Request permission to modify system settings
            requestWriteSettingsPermission()
        }
    }
    private fun getCurrentBrightnessPercent(): Int {
        return try {
            val currentBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            // Convert the current brightness value to percentage
            (currentBrightness * 100) / 255
        } catch (e: Settings.SettingNotFoundException) {
            showToast("Error al obtener el brillo actual: ${e.message}")
            0
        }
    }

    private fun applyBrightness(brightnessValue: Int) {
        try {
            // Set the system brightness using Settings.System API
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightnessValue)
        } catch (e: Settings.SettingNotFoundException) {
            // Handle exception if the setting is not found
            showToast("Error al ajustar el brillo: ${e.message}")
        }
    }

    private fun requestWriteSettingsPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }



}
