package mx.tecnm.tepic.ladm_u4_garciamoreno_asistencia

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.firebase.firestore.FirebaseFirestore
import mx.tecnm.tepic.ladm_u4_garciamoreno_asistencia.databinding.ActivityMainBinding
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    lateinit var binding:ActivityMainBinding
    lateinit var bluetoothAdapter: BluetoothAdapter

    var dispositivos=ArrayList<BluetoothDevice>()
    //lateinit var bluetoothD

    val STATE_LISTENING=1
    val STATE_CONNECTING=2
    val STATE_CONNCETED=3
    val STATE_CONNECTION_FAILED=4
    val STATE_MESSAGE_RECEIVED=5

    var noControl=ArrayList<String>()

    val UUID=java.util.UUID.fromString("62ff97af-13e9-47db-a3ee-9bc1c4d3757b")
    val NOMBRE = "ASISTENCIA"

    var REQUEST_ENABLE_BLUETOOTH=1
    val siPermiso=1

    lateinit var transmision: Transmision

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter?.isEnabled == false) {
            //val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            /*
                if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                    ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),siPermiso)
                //return
            }else {

             */
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
        }

        binding.escuchar.setOnClickListener {
            var server=Server(this)
            server.start()
        }


        binding.consultar.setOnClickListener {
            val ventana = Intent(this,MainActivity2::class.java)
            startActivity(ventana)
        }



    }
    val handler:Handler = object:Handler(){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what){
                STATE_LISTENING->{

                }
                STATE_CONNECTING->{

                }
                STATE_CONNCETED->{
                    Toast.makeText(this@MainActivity,"Conectado como servidor",Toast.LENGTH_LONG).show()
                }
                STATE_CONNECTION_FAILED->{
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage("Error al conectar")
                        .setPositiveButton("Aceptar",{d,i->})
                        .show()
                }
                STATE_MESSAGE_RECEIVED->{
                    var leerBuff:ByteArray = msg.obj as ByteArray
                    var nc = String(leerBuff,0,msg.arg1)
                    //noControl.add(nc)
                    insertar(nc)
                }
            }
        }
    }
    fun insertar(nc:String){
        val bd = FirebaseFirestore.getInstance()
        val datos = hashMapOf(
            "FECHA" to SimpleDateFormat("dd-MM-yyyy").format(Date()).toString(),
            "HORA" to SimpleDateFormat("hh").format(Date()).toString(),
            "NOCONTROL" to nc
        )
        bd.collection("CLASE")
            .add(datos)
            .addOnSuccessListener {
                //SI SE PUDO


            }
            .addOnFailureListener {
                //NO SE PUDO
                AlertDialog.Builder(this)
                    .setMessage(it.message)
                    .show()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==siPermiso){

        }
    }
    @SuppressLint("MissingPermission")
    fun dispositivosConectados():ArrayList<String>{
        var dispositivos = ArrayList<String>()
        /*
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),siPermiso)
            //return
        }else {

         */
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

            for(pD in pairedDevices!!){
                dispositivos.add(pD.name)
            }



        return dispositivos

    }


    @SuppressLint("MissingPermission")
    inner class Server(activity: MainActivity):Thread(){
        private lateinit var serverSocket:BluetoothServerSocket
        var activity=activity
        init {
            try{
                this.serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NOMBRE, UUID)
            }catch (e:Exception){}

        }

        override fun run() {
            super.run()
            var socket:BluetoothSocket?=null
            while (socket==null){
                try {

                    socket = serverSocket.accept()

                   activity.runOnUiThread {
                       Toast.makeText(activity, "Conectado como servidor", Toast.LENGTH_LONG).show()
                   }
                }catch (e:Exception){
                    /*
                    var msg = Message.obtain()
                    msg.what=STATE_CONNECTION_FAILED
                    handler.handleMessage(msg)

                     */
                         activity.runOnUiThread {
                             AlertDialog.Builder(this@MainActivity)
                                 .setMessage("Error al conectar")
                                 .setPositiveButton("Aceptar",{d,i->})
                                 .show()
                         }
                    break

                }

                if(socket!=null){
                    /*
                    var msg = Message.obtain()
                    msg.what=STATE_CONNCETED
                    handler.handleMessage(msg)
                    */
                     activity.runOnUiThread {
                         Toast.makeText(this@MainActivity,"Conectado como servidor",Toast.LENGTH_LONG).show()
                         binding.conexion.setText("Servidor")
                     }
                    transmision=Transmision(activity,socket)
                    transmision.start()
                    break
                }
            }
        }
        fun cancelar(){
            try{
                serverSocket.close()
            }catch (e:Exception){

            }
        }
    }
    inner class Transmision(private val activity: MainActivity, private val socket: BluetoothSocket):Thread(){
        private val inputStream = this.socket.inputStream
        private val outputStream = this.socket.outputStream
        private var mmBuffer: ByteArray = ByteArray(1024)

        override fun run() {
            try {
                val available = inputStream.available()
                val bytes = ByteArray(available)
                Log.i("server", "Reading")
                try{
                    inputStream.read(mmBuffer)
                }catch(e: IOException){
                    activity.runOnUiThread {
                        AlertDialog.Builder(activity)
                            .setTitle("Error")
                            .setMessage("Error al tomar lista")
                            .show()
                    }
                    return
                }
                val text = String(mmBuffer)
                Log.i("server", "Message received")
                Log.i("server", "MENSAJE: ${text}")
                activity.appendText(text)
            } catch (e: Exception) {
                Log.e("client", "Cannot read data", e)
            } finally {
                inputStream.close()
                outputStream.close()
                socket.close()
            }
        }
    }
    /*
    fun btListen():BluetoothServerSocket{
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),siPermiso)
            return btListen()
        }else
            return bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NOMBRE,UUID)
    }*/
    fun appendText(text:String){
        runOnUiThread {
            insertar(text)
        }
    }
}