package com.clipp.devicemanagerapp

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*
import android.bluetooth.BluetoothAdapter
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.IntentFilter

/**
 * MainActivity es la actividad principal de la aplicación Device Manager.
 * Proporciona funciones para controlar varios aspectos del dispositivo,
 * como reinicio, apagado, Bluetooth, GPS, datos móviles, pantalla, modo avión,
 * modo quiosco, volumen, brillo y configuración de fecha/hora.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private lateinit var audioManager: AudioManager
    private lateinit var powerManager: PowerManager

    companion object {
        const val REQUEST_CODE = 123
        const val PREFS_NAME = "MyPrefs"
        const val FIRST_RUN_KEY = "first_run"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeServices()
        initializeButtons()

        if (isFirstRun()) {
            requestNecessaryPermissions()
            requestDeviceAdmin()
            setFirstRunFalse()
        }

    }

    /**
     * Inicializa los servicios del sistema necesarios.
     */
    private fun initializeServices() {
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, DeviceAdminReceiver::class.java)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    /**
     * Inicializa los botones de la interfaz y configura sus listeners.
     */
    private fun initializeButtons() {
        findViewById<Button>(R.id.btnReboot).setOnClickListener { rebootDevice() }
        findViewById<Button>(R.id.btnShutdown).setOnClickListener { shutdownDevice() }
        findViewById<Button>(R.id.btnEnableBluetooth).setOnClickListener { setBluetooth(true) }
        findViewById<Button>(R.id.btnDisableBluetooth).setOnClickListener { setBluetooth(false) }
        findViewById<Button>(R.id.btnEnableGPS).setOnClickListener { setGPS() }
        findViewById<Button>(R.id.btnDisableGPS).setOnClickListener { setGPS() }
        findViewById<Button>(R.id.btnEnableMobileData).setOnClickListener { setMobileData(true) }
        findViewById<Button>(R.id.btnDisableMobileData).setOnClickListener { setMobileData(false) }
        findViewById<Button>(R.id.btnEnableScreen).setOnClickListener { setScreenOn(true) }
        findViewById<Button>(R.id.btnDisableScreen).setOnClickListener { setScreenOn(false) }
        findViewById<Button>(R.id.btnEnableAirplaneMode).setOnClickListener { setAirplaneMode(true) }
        findViewById<Button>(R.id.btnDisableAirplaneMode).setOnClickListener { setAirplaneMode(false) }
        findViewById<Button>(R.id.btnEnableKioskMode).setOnClickListener { setKioskMode(true) }
        findViewById<Button>(R.id.btnDisableKioskMode).setOnClickListener { setKioskMode(false) }
        findViewById<Button>(R.id.btnAdjustVolume).setOnClickListener { showVolumeDialog() }
        findViewById<Button>(R.id.btnAdjustBrightness).setOnClickListener { showBrightnessDialog() }
        findViewById<Button>(R.id.btnChangeDateTime).setOnClickListener { showDateTimePicker() }
    }

    /**
     * Solicita los permisos necesarios para el funcionamiento de la aplicación.
     */
    private fun requestNecessaryPermissions() {
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
    }

    /**
     * Solicita permisos si aún no han sido concedidos.
     */
    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        val permissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded, REQUEST_CODE)
        }
    }

    /**
    * Verifica si es la primera vez que la aplicación se está ejecutando.
    */
    private fun isFirstRun(): Boolean {
        val preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean(FIRST_RUN_KEY, true)
    }

    /**
     * Marca la aplicación como ya iniciada por primera vez.
     */
    private fun setFirstRunFalse() {
        val preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(preferences.edit()) {
            putBoolean(FIRST_RUN_KEY, false)
            apply()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_BLUETOOTH) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permisos concedidos
                showToast("Permisos de Bluetooth concedidos")
            } else {
                // Permisos denegados
                showToast("Permisos de Bluetooth denegados")
            }
        }
        if (requestCode == REQUEST_CODE) {
            val deniedPermissions = permissions.zip(grantResults.toTypedArray())
                .filter { it.second != PackageManager.PERMISSION_GRANTED }
                .map { it.first }

            if (deniedPermissions.isNotEmpty()) {
                showToast("Los siguientes permisos son necesarios: $deniedPermissions")
            } else {
                showToast("Todos los permisos concedidos")
            }
        }
    }

    /**
     * Solicita permisos de administrador del dispositivo.
     */
    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
        intent.putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Permite activar el modo quiosco."
        )
        startActivityForResult(intent, 1)
    }

    /**
     * Activa o desactiva el modo avión.
     */
    private fun setAirplaneMode(enable: Boolean) {
        try {
            Settings.System.putInt(
                contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                if (enable) 1 else 0
            )
        } catch (e: Exception) {
            showToast("Exception occurred during Airplane Mode change")
            e.printStackTrace()
        }
    }

    /**
     * Muestra un diálogo para ajustar el volumen del dispositivo.
     */
    private fun showVolumeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_volume_control, null)
        val seekBarVolume = dialogView.findViewById<SeekBar>(R.id.seekBarVolume)
        val tvVolume = dialogView.findViewById<TextView>(R.id.tvVolume)

        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        seekBarVolume.progress = currentVolume
        tvVolume.text = "Ajustar Volumen: $currentVolume"

        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    progress,
                    AudioManager.FLAG_PLAY_SOUND
                )
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

    /**
     * Muestra un diálogo para ajustar el brillo de la pantalla.
     */
    private fun showBrightnessDialog() {
        if (Settings.System.canWrite(this)) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_brightness_control, null)
            val seekBarBrightness = dialogView.findViewById<SeekBar>(R.id.seekBarBrightness)
            val tvBrightness = dialogView.findViewById<TextView>(R.id.tvBrightness)

            seekBarBrightness.max = 100
            val currentBrightness = getCurrentBrightnessPercent()
            seekBarBrightness.progress = currentBrightness
            tvBrightness.text = "Ajustar Brillo: $currentBrightness%"

            seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val brightnessValue = (progress * 255) / 100
                    applyBrightness(brightnessValue)
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
            requestWriteSettingsPermission()
        }
    }

    /**
     * Obtiene el porcentaje actual de brillo de la pantalla.
     */
    private fun getCurrentBrightnessPercent(): Int {
        return try {
            val currentBrightness =
                Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            (currentBrightness * 100) / 255
        } catch (e: Settings.SettingNotFoundException) {
            showToast("Error al obtener el brillo actual: ${e.message}")
            0
        }
    }

    /**
     * Aplica el valor de brillo especificado a la pantalla.
     */
    private fun applyBrightness(brightnessValue: Int) {
        try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightnessValue
            )
        } catch (e: Settings.SettingNotFoundException) {
            showToast("Error al ajustar el brillo: ${e.message}")
        }
    }

    /**
     * Solicita permiso para modificar los ajustes del sistema.
     */
    private fun requestWriteSettingsPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    /**
     * Muestra un mensaje Toast.
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Reinicia el dispositivo.
     */
    private fun rebootDevice() {
        if (devicePolicyManager.isAdminActive(compName)) {
            devicePolicyManager.reboot(compName)
            showToast("Reiniciando dispositivo")
        } else {
            showToast("Permiso de administrador de dispositivo requerido para reiniciar")
        }
    }

    /**
     * Apaga el dispositivo.
     */
    private fun shutdownDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            showToast("Este dispositivo no tiene permisos para apagar automáticamente")
        } else {
            val intent = Intent(Intent.ACTION_SHUTDOWN)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    /**
     * Activa o desactiva el Bluetooth.
     */
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_CODE_BLUETOOTH = 101

    private fun setBluetooth(enable: Boolean) {
        // Verifica permisos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_CONNECT), REQUEST_CODE_BLUETOOTH)
            return
        }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            showToast("Bluetooth no está disponible en este dispositivo.")
            return
        }

        if (enable) {
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                showToast("Bluetooth ya está activado.")
            }
        } else {
            if (bluetoothAdapter.isEnabled) {
                showToast("Intentando desactivar Bluetooth...")
                bluetoothAdapter.disable()

                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val state = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        if (state == BluetoothAdapter.STATE_OFF) {
                            showToast("Bluetooth desactivado.")
                            unregisterReceiver(this)
                        } else if (state == BluetoothAdapter.STATE_ON) {
                            showToast("Error al desactivar Bluetooth.")
                            unregisterReceiver(this)
                        }
                    }
                }
                val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                registerReceiver(receiver, filter)
            } else {
                showToast("Bluetooth ya está desactivado.")
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                showToast("Bluetooth activado")
            } else {
                showToast("El usuario canceló la solicitud de encender Bluetooth")
            }
        }
    }

    /**
     * Abre la configuración de GPS.
     */
    private fun setGPS() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    /**
     * Abre la configuración de datos móviles.
     */
    private fun setMobileData(enable: Boolean) {
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /**
     * Enciende o apaga la pantalla.
     */
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

    /**
     * Activa o desactiva el modo quiosco.
     */
    private fun setKioskMode(enable: Boolean) {
        if (enable) {
            startLockTask()
            showToast("Modo quiosco activado")
        } else {
            stopLockTask()
            showToast("Modo quiosco desactivado")
        }
    }

    /**
     * Muestra un selector de fecha y hora.
     */
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
                        val selectedDateTime = findViewById<TextView>(R.id.selectedDateTime)
                        selectedDateTime.text = calendar.time.toString()
                        setSystemDateTime(calendar)
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

    /**
     * Establece la fecha y hora del sistema.
     */
    private fun setSystemDateTime(calendar: Calendar) {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("Cambiar Fecha y Hora")
                .setMessage("No es posible cambiar la fecha y hora automáticamente debido a restricciones de permisos. Por favor, cambie la fecha y hora manualmente en la configuración del sistema.")
                .setPositiveButton("Ir a Configuración") { _, _ ->
                    startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

}