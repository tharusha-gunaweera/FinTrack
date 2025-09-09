package com.example.fintrack

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var buttonNext: MaterialButton
    private lateinit var buttonGetStarted: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_onboarding)

            viewPager = findViewById(R.id.viewPager)
            buttonNext = findViewById(R.id.buttonNext)
            buttonGetStarted = findViewById(R.id.buttonGetStarted)

            val onboardingItems = listOf(
                OnboardingItem(
                    "Track Your Finances",
                    "Monitor your income and expenses in one place with beautiful visualizations",
                    R.drawable.ic_onboarding_1
                ),
                OnboardingItem(
                    "Smart Budgeting",
                    "Set budgets and get alerts when you're approaching your limits",
                    R.drawable.ic_onboarding_2
                ),
                OnboardingItem(
                    "Detailed Reports",
                    "Get insights into your spending patterns with detailed reports",
                    R.drawable.ic_onboarding_3
                ),
                OnboardingItem(
                    "Secure & Private",
                    "Your data stays on your device with bank-level encryption",
                    R.drawable.ic_onboarding_4
                )
            )

            viewPager.adapter = OnboardingAdapter(onboardingItems)

            buttonNext.setOnClickListener {
                if (viewPager.currentItem < onboardingItems.size - 1) {
                    viewPager.currentItem += 1
                }
            }

            buttonGetStarted.setOnClickListener {
                // Navigate to your main activity
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if (position == onboardingItems.size - 1) {
                        buttonNext.visibility = View.GONE
                        buttonGetStarted.visibility = View.VISIBLE
                    } else {
                        buttonNext.visibility = View.VISIBLE
                        buttonGetStarted.visibility = View.GONE
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("OnboardingActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private inner class OnboardingAdapter(private val items: List<OnboardingItem>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
            return OnboardingViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_onboarding,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
            try {
                holder.bind(items[position])
            } catch (e: Exception) {
                Log.e("OnboardingAdapter", "Error binding view holder: ${e.message}", e)
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        inner class OnboardingViewHolder(itemView: View) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            private val image: ImageView = itemView.findViewById(R.id.imageOnboarding)
            private val title: TextView = itemView.findViewById(R.id.textTitle)
            private val description: TextView = itemView.findViewById(R.id.textDescription)

            fun bind(onboardingItem: OnboardingItem) {
                try {
                    image.setImageResource(onboardingItem.image)
                    title.text = onboardingItem.title
                    description.text = onboardingItem.description
                } catch (e: Exception) {
                    Log.e("OnboardingViewHolder", "Error binding item: ${e.message}", e)
                }
            }
        }
    }

    data class OnboardingItem(
        val title: String,
        val description: String,
        val image: Int
    )
}