package mx.tecnm.tepic.ladm_u4_garciamoreno_asistencia

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import mx.tecnm.tepic.ladm_u4_garciamoreno_asistencia.databinding.ActivityMain2Binding
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.lang.Exception
import java.net.URI
import java.nio.file.Paths

class MainActivity2 : AppCompatActivity() {
    //lateinit var binding: mx.tecnm.tepic.ladm_u4_garciamoreno_asistencia.databinding.ActivityMain2Binding
    lateinit var binding:ActivityMain2Binding

    val CREATE_FILE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.consultar.setOnClickListener {
            //createFile()
            FirebaseFirestore.getInstance()
                .collection("CLASE")
                .whereEqualTo("FECHA",binding.fecha.text.toString())
                .whereEqualTo("HORA",binding.hora.text.toString())
                .addSnapshotListener { value, error ->
                    if(error!=null){
                        //ERROR
                        AlertDialog.Builder(this)
                            .setMessage(error.message)
                            .show()
                        return@addSnapshotListener
                    }
                    crearArchivo(value!!)

                }
        }
        binding.fecha.setOnClickListener {
            showDatePickerDialog()
        }
    }

    fun crearArchivo(value: QuerySnapshot){
        var nombre = "formato_asistencia.csv"
        try {


            var archivo = OutputStreamWriter(
                openFileOutput(
                    nombre,
                    MODE_PRIVATE
                )
            )
            var cadena = "${binding.fecha.text.toString()},${binding.hora.text.toString()}\n"
            for (documento in value!!) {
                cadena += documento.getString("NOCONTROL")
                cadena += "\n"
            }
            archivo.write(cadena)
            archivo.flush()
            archivo.close()
            println("Write CSV successfully!")
        }catch (e:Exception){
            println("Writing CSV error!")
            e.printStackTrace()
        }
        val sendIntent = Intent()
        val file: File = File(this.getFilesDir(), nombre)
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this,
            "mx.tecnm.tepic.ladm_u4_garciamoreno_asistencia.provider",file))
        sendIntent.type = "text/csv"
        startActivity(Intent.createChooser(sendIntent, "SHARE"))
    }






    private fun showDatePickerDialog() {
        val newFragment = DatePickerFragment.newInstance(DatePickerDialog.OnDateSetListener { _, year, month, day ->
            // +1 because January is zero
            //val selectedDate = day.toString() + " / " + (month + 1) + " / " + year
            var m =""
            if (month+1<10) m = "0"+(month+1) else m=""+(month+1)
            var d = ""
            if (day<10) d = "0"+day else d = ""+d

            val selectedDate = year.toString()+"-"+ m +"-"+d
            binding.fecha.setText(selectedDate)
        })

        newFragment.show(supportFragmentManager, "datePicker")
    }
}