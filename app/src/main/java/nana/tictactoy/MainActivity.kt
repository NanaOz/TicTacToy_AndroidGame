package nana.tictactoy

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import nana.tictactoy.databinding.ActivityMainBinding

const val EXTRA_TIME = "nana.tictactoy.TIME"
const val EXTRA_GAME_FIELD = "nana.tictactoy.GAME_FIELD"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding;
    override fun onCreate(savedInstanceState: Bundle?) {

        setTheme(R.style.Theme_TicTacToy)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        binding.newGame.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        binding.continueGame.setOnClickListener {
            val gameInfo = getInfoAboutLastGame()
            val intent = Intent(this, GameActivity::class.java).apply {
                putExtra(EXTRA_TIME, gameInfo.time)
                putExtra(EXTRA_GAME_FIELD, gameInfo.gameField)
            }
            startActivity(intent)
        }

        binding.settings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        setContentView(binding.root)
    }

    private fun getInfoAboutLastGame(): InfoGame {
        with(getSharedPreferences("game", MODE_PRIVATE)) {
            val time = getLong("time", 0)
            val gameField = getString("gameField", "")

            return if (gameField != null) {
                InfoGame(time, gameField)
            } else {
                InfoGame(0, "")
            }
        }
    }

    data class InfoGame(val time: Long, val gameField: String)
}