package com.example.nimons360_far.ui.screens

import android.os.Bundle
import com.example.nimons360_far.R
import com.example.nimons360_far.ui.base.BaseActivity
import com.example.nimons360_far.ui.families.Families

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Families())
                .commit()
        }
    }
}