package com.clipp.Logica

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
import android.util.Log
import com.clipp.devicemanagerapp.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

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
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        const val REQUEST_CODE = 123
        const val PREFS_NAME = "MyPrefs"
        const val FIRST_RUN_KEY = "first_run"
        const val REQUEST_CODE_WRITE_SETTINGS = 200
        const val REQUEST_CODE_BATTERY_SAVER = 2
        private const val ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE = 0

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


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()

        // Llamada para solicitar permisos y desactivar Bluetooth
        requestNecessaryPermissions()

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
        findViewById<Button>(R.id.btnGPS).setOnClickListener { GPS() }
        findViewById<Button>(R.id.btnAirplaneMode).setOnClickListener { AirplaneMode() }
        findViewById<Button>(R.id.btnMobileData).setOnClickListener { MobileData() }
        findViewById<Button>(R.id.btnReboot).setOnClickListener { rebootDevice() }
        findViewById<Button>(R.id.btnShutdown).setOnClickListener { shutdownDevice() }
        findViewById<Button>(R.id.btnEnableBluetooth).setOnClickListener { setBluetooth(true) }
        findViewById<Button>(R.id.btnDisableBluetooth).setOnClickListener { setBluetooth(false) }
        findViewById<Button>(R.id.btnEnableScreen).setOnClickListener { setScreenOn(true) }
        findViewById<Button>(R.id.btnDisableScreen).setOnClickListener { setScreenOn(false) }
        findViewById<Button>(R.id.btnEnableKioskMode).setOnClickListener { setKioskMode(true) }
        findViewById<Button>(R.id.btnDisableKioskMode).setOnClickListener { setKioskMode(false) }
        findViewById<Button>(R.id.btnAdjustVolume).setOnClickListener { showVolumeDialog() }
        findViewById<Button>(R.id.btnAdjustBrightness).setOnClickListener { showBrightnessDialog() }
        findViewById<Button>(R.id.btnChangeDateTime).setOnClickListener { showDateTimePicker() }
        findViewById<Button>(R.id.btnBatterySaver).setOnClickListener { setBatterySaver() }

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
            Manifest.permission.EXPAND_STATUS_BAR,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
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
        } else {
            setBluetooth(false) // Permisos ya concedidos, intenta desactivar Bluetooth
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
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

        if (requestCode == ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                showPermissionRationale()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
                // Redirige al usuario a la configuración de Bluetooth
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                startActivity(intent)
                showToast("Por favor, desactive Bluetooth manualmente desde los ajustes.")
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
        } else if (requestCode == REQUEST_CODE_WRITE_SETTINGS) {
            if (Settings.System.canWrite(this)) {
                showToast("Permiso de modificar configuraciones del sistema concedido")
            } else {
                showToast("Permiso de modificar configuraciones del sistema denegado")
            }
        } else if (requestCode == REQUEST_CODE_BATTERY_SAVER) {
            showToast("Vuelva a la aplicación después de habilitar el Ahorro de Batería")
        }
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

    /**
     * Activar y desactivar el ahorro de batería
     */
    private fun setBatterySaver() {
        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        try {
            startActivity(intent)
            showToast("Abrir configuración de Ahorro de Batería")
        } catch (e: Exception) {
            showToast("No se puede abrir la configuración de Ahorro de Batería")
            e.printStackTrace()
        }
    }

    private fun requestWriteSettingsPermission2() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS)
    }

    /**
     * Activar y desactivar gps
     */
    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
    }

    override fun onStop() {
        super.onStop()

    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Acceder a la ubicación del teléfono")
            .setMessage("Se necesita acceso a la ubicación para proporcionar esta funcionalidad. Por favor, conceda el permiso.")
            .setPositiveButton("Aceptar") { _, _ ->
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), ACCESS_FINE_LOCATION_PERMISSION_REQUEST_CODE)
            }
            .show()
    }

    private fun GPS() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                if (task.isSuccessful && task.result != null) {

                } else {

                }
            }
        }
    }

    /**
     * Activar y desactivar modo avión
     */

    private fun AirplaneMode() {
        val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        startActivity(intent)
    }

    /**
     * Activar y desactivar datos móviles
     */

    private fun MobileData() {
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
        startActivity(intent)
    }

}