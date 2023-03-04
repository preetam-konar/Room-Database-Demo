package com.konar.roomdemo

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.konar.roomdemo.databinding.ActivityMainBinding
import com.konar.roomdemo.databinding.DialogUpdateBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        //EmployeeDao
        val employeeDao = (application as EmployeeApp).db.employeeDao()
        binding?.btnAdd?.setOnClickListener {
            addRecord(employeeDao)
        }

        lifecycleScope.launch {
            employeeDao.fetchAllEmployees().collect {
                val list = ArrayList(it)
                setupListOfDataIntoRecyclerView(list, employeeDao)
            }
        }

    }

    //RecyclerView
    private fun setupListOfDataIntoRecyclerView(
        employeeList: ArrayList<EmployeeEntity>,
        employeeDao: EmployeeDao
    ) {
        if (employeeList.isNotEmpty()) {
            val itemAdapter = ItemAdapter(
                employeeList,
                { updateId ->
                    updateRecordDialog(updateId, employeeDao)
                }, { deleteId ->
                    deleteRecordAlertDialog(deleteId, employeeDao)
                }
            )

            binding?.rvItemsList?.layoutManager = LinearLayoutManager(this)
            binding?.rvItemsList?.adapter = itemAdapter
            binding?.rvItemsList?.visibility = View.VISIBLE
            binding?.tvNoRecordsAvailable?.visibility = View.GONE
        } else {
            binding?.rvItemsList?.visibility = View.GONE
            binding?.tvNoRecordsAvailable?.visibility = View.VISIBLE
        }
    }

    //Update Dialog
    private fun updateRecordDialog(id: Int, employeeDao: EmployeeDao) {
        val updateDialog = Dialog(this, R.style.my_dialog)
        updateDialog.setCancelable(false)
        val binding = DialogUpdateBinding.inflate(layoutInflater)
        updateDialog.setContentView(binding.root)

        lifecycleScope.launch {
            employeeDao.fetchEmployeeById(id).collect {
                if (it != null) {
                    binding.etUpdateName.setText(it.name)
                    binding.etUpdateEmail.setText(it.email)
                }
            }
        }


        binding.tvUpdate.setOnClickListener {
            val name = binding.etUpdateName.text.toString()
            val email = binding.etUpdateEmail.text.toString()
            if (name.isNotEmpty() && email.isNotEmpty()) {
                lifecycleScope.launch {
                    employeeDao.update(EmployeeEntity(id, name = name, email = email))
                    Toast.makeText(applicationContext, "Record updated", Toast.LENGTH_SHORT).show()
                    updateDialog.dismiss()
                }
            } else {
                Toast.makeText(
                    applicationContext,
                    "Name or email cannot be blank",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.tvCancel.setOnClickListener {
            updateDialog.dismiss()
        }
        updateDialog.show()
    }

    //Delete Dialog
    private fun deleteRecordAlertDialog(id: Int, employeeDao: EmployeeDao) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Record")
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        lifecycleScope.launch {
            employeeDao.fetchEmployeeById(id).collect {
                if (it != null) {
                    builder.setMessage("Are you sure you want to delete ${it.name}")
                }
            }
        }

        builder.setPositiveButton("Yes") { dialogInterface, _ ->
            lifecycleScope.launch {
                employeeDao.delete(EmployeeEntity(id))
                Toast.makeText(
                    applicationContext,
                    "Record deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
            dialogInterface.dismiss()
        }

        builder.setNegativeButton("No") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }

        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)

        alertDialog.show()
    }

    private fun addRecord(employeeDao: EmployeeDao) {
        val name = binding?.etName?.text.toString()
        val email = binding?.etEmail?.text.toString()

        if (name.isNotEmpty() && email.isNotEmpty()) {
            lifecycleScope.launch {
                employeeDao.insert(EmployeeEntity(name = name, email = email))
            }
            Toast.makeText(applicationContext, "Record Saved", Toast.LENGTH_SHORT).show()
            binding?.etName?.text?.clear()
            binding?.etEmail?.text?.clear()
        } else {
            Toast.makeText(this, "Name or Email can't be blank!", Toast.LENGTH_SHORT).show()
        }
    }

}