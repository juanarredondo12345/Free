package com.example.aimbotnodpi

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var etIP: EditText
    private lateinit var btnConnect: Button
    private lateinit var btnToggle: Button

    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var isConnected = false
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vistas
        tvStatus = findViewById(R.id.tvStatus)
        etIP = findViewById(R.id.etIP)
        btnConnect = findViewById(R.id.btnConnect)
        btnToggle = findViewById(R.id.btnToggle)

        // Cargar IP guardada
        etIP.setText(loadPref("server_ip", "192.168.1.100")) 

        // Botón Conectar
        btnConnect.setOnClickListener {
            val ip = etIP.text.toString().trim()
            if (ip.isEmpty()) return@setOnClickListener
            
            savePref("server_ip", ip)
            connectToServer(ip)
        }

        // Botón Toggle
        btnToggle.setOnClickListener {
            if (!isConnected) {
                showToast("Primero conecta")
                return@setOnClickListener
            }
            
            val isOn = btnToggle.text == "DESACTIVAR AIMBOT"
            sendCommand(if (isOn) "AIM_OFF" else "AIM_ON")
            
            // Actualizar UI
            if (isOn) {
                btnToggle.text = "ACTIVAR AIMBOT"
                btnToggle.setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
            } else {
                btnToggle.text = "DESACTIVAR AIMBOT"
                btnToggle.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
            }
        }
    }

    private fun connectToServer(ip: String) {
        updateStatus("Conectando...", "#FF9800")
        btnConnect.isEnabled = false

        scope.launch {
            try {
                socket = Socket()
                // Timeout de 3 segundos
                socket?.connect(InetSocketAddress(ip, 8080), 3000) 
                
                if (socket?.isConnected == true) {
                    outputStream = DataOutputStream(socket?.getOutputStream())
                    isConnected = true
                    
                    runOnUiThread {
                        updateStatus("Conectado: $ip", "#4CAF50")
                        btnConnect.isEnabled = true
                        btnToggle.isEnabled = true
                        showToast("¡Conexión exitosa!")
                    }
                } else {
                    throw Exception("Timeout")
                }
            } catch (e: Exception) {
                isConnected = false
                socket?.close()
                runOnUiThread {
                    updateStatus("Error: ${e.message}", "#F44336")
                    btnConnect.isEnabled = true
                    btnToggle.isEnabled = false
                }
            }
        }
    }

    private fun sendCommand(command: String) {
        scope.launch {
            try {
                outputStream?.writeBytes("$command\n")
                outputStream?.flush()
                showToast("Enviado: $command")
            } catch (e: Exception) {
                runOnUiThread {
                    updateStatus("Desconectado", "#F44336")
                    isConnected = false
                    btnToggle.isEnabled = false
                }
            }
        }
    }

    private fun updateStatus(text: String, colorHex: String) {
        tvStatus.text = text
        tvStatus.setTextColor(android.graphics.Color.parseColor(colorHex))
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    // Preferencias
    private fun savePref(key: String, value: String) {
        getSharedPreferences("config", Context.MODE_PRIVATE)
            .edit().putString(key, value).apply()
    }

    private fun loadPref(key: String, default: String): String {
        return getSharedPreferences("config", Context.MODE_PRIVATE)
            .getString(key, default) ?: default
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        socket?.close()
    }
}
