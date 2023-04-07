package nana.tictactoy

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import nana.tictactoy.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private var currentSoundValue = 0
    private var currentLvl = 0
    private var currentRules = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)

        val data = getSettingsInfo()

        currentSoundValue = data.soundValue
        currentLvl = data.lvl
        currentRules = data.rules

        binding.prevLvl.setOnClickListener {
            currentLvl--

            updateLvl(currentLvl)
        }

        binding.nextLvl.setOnClickListener {
            currentLvl++

            updateLvl(currentLvl)
        }

        binding.soundBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, value: Int, p2: Boolean) {
                currentSoundValue = value
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                updateSoundValue(currentSoundValue)
            }
        })

        binding.checkBoxVertical.setOnCheckedChangeListener{_, isChecked ->
            if(isChecked){
                currentRules++
            } else {
                currentRules--
            }

            updateRules(currentRules)
        }

        binding.checkBoxHorizontal.setOnCheckedChangeListener{_, isChecked ->
            if(isChecked){
                currentRules+=2
            } else {
                currentRules-=2
            }

            updateRules(currentRules)
        }

        binding.checkBoxDiagonal.setOnCheckedChangeListener{_, isChecked ->
            if(isChecked){
                currentRules+=4
            } else {
                currentRules-=4
            }

            updateRules(currentRules)
        }

        setContentView(binding.root)
    }

    private fun updateSoundValue(value: Int) {
        with(getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE).edit()){
            putInt("sound_value", value)
            apply()
        }
    }
    private fun updateLvl(lvl: Int) {
        with(getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE).edit()){
            putInt("lvl", lvl)
            apply()
        }
    }
    private fun updateRules(rules: Int) {
        with(getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE).edit()){
            putInt("rules", rules)
            apply()
        }
    }

    private fun getSettingsInfo() : SettingsInfo {
        with(getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)) {
            val soundValue = getInt("sound_value", 0)
            val lvl = getInt("lvl", 0)
            val rules = getInt("rules", 0)

            return SettingsInfo(soundValue, lvl, rules)
        }
    }

    data class SettingsInfo(val soundValue: Int, val lvl: Int, val rules: Int)
}