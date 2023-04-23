package nana.tictactoy

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import nana.tictactoy.databinding.ActivityGameBinding

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding

    private lateinit var gameField: Array<Array<String>>

    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGameBinding.inflate(layoutInflater)

        binding.toGameClose.setOnClickListener {
//            onBackPressed()  //устарел, заменила на
            onBackPressedDispatcher.onBackPressed()
        }

        binding.toPopupMenu.setOnClickListener {
            showPopupMenu()
        }

        binding.cell11.setOnClickListener {
            makeStepOfUser(0, 0)
        }
        binding.cell12.setOnClickListener {
            makeStepOfUser(0, 1)
        }
        binding.cell13.setOnClickListener {
            makeStepOfUser(0, 2)
        }
        binding.cell21.setOnClickListener {
            makeStepOfUser(1, 0)
        }
        binding.cell22.setOnClickListener {
            makeStepOfUser(1, 1)
        }
        binding.cell23.setOnClickListener {
            makeStepOfUser(1, 2)
        }
        binding.cell31.setOnClickListener {
            makeStepOfUser(2, 0)
        }
        binding.cell32.setOnClickListener {
            makeStepOfUser(2, 1)
        }
        binding.cell33.setOnClickListener {
            makeStepOfUser(2, 2)
        }
        setContentView(binding.root)

        val time = intent.getLongExtra(EXTRA_TIME, 0L)
        val gameField = intent.getStringExtra(EXTRA_GAME_FIELD)

        if (gameField != null && time != 0L && gameField != "") {
            restartGame(time, gameField)
        } else {
            initGameField()
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.mus)
        mediaPlayer.isLooping = true
        val settingsInfo = getCurrentSettings()
        setVolumeMediaPlayer(settingsInfo.soundValue)

        binding.chronometer.start()
        mediaPlayer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.release()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == POPUP_MENU) {
            if (resultCode == RESULT_OK) {
                mediaPlayer = MediaPlayer.create(this, R.raw.mus)
                mediaPlayer.isLooping = true
                val settingsInfo = getCurrentSettings()
                setVolumeMediaPlayer(settingsInfo.soundValue)

                binding.chronometer.start()
                mediaPlayer.start()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setVolumeMediaPlayer(soundValue: Int) {
        val volume = soundValue / 100.0
        mediaPlayer.setVolume(volume.toFloat(), volume.toFloat())
    }

    private fun restartGame(time: Long, gameField: String) {
        binding.chronometer.base = SystemClock.elapsedRealtime() - time

        this.gameField = arrayOf()

        val rows = gameField.split("\n")

        for (row in rows) {
            val columns = row.split(";")
            this.gameField += columns.toTypedArray()
        }

        this.gameField.forEachIndexed { indexRow, columns ->
            columns.forEachIndexed { indexColumn, cell ->
                makeGameFieldUI("$indexRow$indexColumn", cell)
            }
        }
    }

    private fun convertGameFieldToString(): String {
        val tmpArray = arrayListOf<String>()
        gameField.forEach { tmpArray.add(it.joinToString(separator = ";")) }
        return tmpArray.joinToString(separator = "\n")
    }

    private fun saveGame(time: Long, gameField: String) {
        getSharedPreferences("game", MODE_PRIVATE).edit().apply {
            putLong("time", time)
            putString("gameField", gameField)
            apply()
        }
    }

    private fun initGameField() {
        gameField = arrayOf()

        for (i in 0..2) {
            var array = arrayOf<String>()
            for (j in 0..2) {
                array += " "
            }
            gameField += array
        }
    }

    private fun makeStepOfUser(row: Int, col: Int) {
        if (isEmptyField(row, col)) {
            makeStep(row, col, PLAYER_SYMBOL)

            if (checkGameField(row, col, PLAYER_SYMBOL)) {
                showGameStatus(STATUS_PLAYER_WIN)
            } else if (!isFilledGame()) {
                val stepOfAI = makeStepOfAI()

                if (checkGameField(stepOfAI.row, stepOfAI.col, BOT_SYMBOL)) {
                    showGameStatus(STATUS_PLAYER_LOSE)
                } else if (isFilledGame()) {
                    showGameStatus(STATUS_PLAYER_DRAW)
                }
            } else {
                showGameStatus(STATUS_PLAYER_DRAW)
            }
        } else {
            Toast.makeText(this, "Поле уже заполнено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeStepOfAI(): CellGameField {
        val settingsInfo = getCurrentSettings()
        return when (settingsInfo.lvl) {
            0 -> makeStepOfAIEasyLvl()
            1 -> makeStepOfAIMediumLvl()
            2 -> makeStepOfAIHardLvl()
            else -> CellGameField(0, 0)
        }
    }

    private fun makeStepOfAIEasyLvl(): CellGameField {
        var randRow = 0
        var randCol = 0

        do {
            randRow = (0..2).random()
            randCol = (0..2).random()
        } while (!isEmptyField(randRow, randCol))

        makeStep(randRow, randCol, BOT_SYMBOL)

        return CellGameField(randRow, randCol)
    }

    private fun makeStepOfAIMediumLvl(): CellGameField {
        var bestScore = Double.NEGATIVE_INFINITY
        var move = CellGameField(0, 0)

        val board = gameField.map { it.clone() }.toTypedArray()

        board.forEachIndexed { indexRow, cols ->
            cols.forEachIndexed { indexCol, cell ->
                if (board[indexRow][indexCol] == " ") {
                    board[indexRow][indexCol] = BOT_SYMBOL
                    val score = minimax(board, false)
                    board[indexRow][indexCol] = " "
                    if (score > bestScore) {
                        bestScore = score
                        move = CellGameField(indexRow, indexCol)
                    }
                }
            }
        }
        makeStep(move.row, move.col, BOT_SYMBOL)

        return move
    }

    private fun makeStepOfAIHardLvl(): CellGameField {
        var bestScore = Double.NEGATIVE_INFINITY
        var move = CellGameField(0, 0)

        var board = gameField.map { it.clone() }.toTypedArray()

        board.forEachIndexed { indexRow, cols ->
            cols.forEachIndexed { indexCol, cell ->
                if (board[indexRow][indexCol] == " ") {
                    board[indexRow][indexCol] = BOT_SYMBOL
                    val score = minimax(board, false)
                    board[indexRow][indexCol] = " "
                    if (score > bestScore) {
                        bestScore = score
                        move = CellGameField(indexRow, indexCol)
                    }
                }
            }
        }
        makeStep(move.row, move.col, BOT_SYMBOL)

        return move
    }

    private fun minimax(board: Array<Array<String>>, isMaximazing: Boolean): Double {
        val result = checkWinner(board)
        result?.let {
//            return scores[result]!!
            return (scores[result] as Double?)!!
        }
        if (isMaximazing) {
            var bestScore = Double.NEGATIVE_INFINITY
            board.forEachIndexed { indexRow, cols ->
                cols.forEachIndexed { indexCol, cell ->
                    if (board[indexRow][indexCol] == " ") {
                        board[indexRow][indexCol] = BOT_SYMBOL
                        val score = minimax(board, false)
                        board[indexRow][indexCol] = " "
                        if (score > bestScore) {
                            bestScore = score
                        }
                    }
                }
            }
            return bestScore
        } else {
            var bestScore = Double.POSITIVE_INFINITY
            board.forEachIndexed { indexRow, cols ->
                cols.forEachIndexed { indexCol, cell ->
                    if (board[indexRow][indexCol] == " ") {
                        board[indexRow][indexCol] = PLAYER_SYMBOL
                        val score = minimax(board, true)
                        board[indexRow][indexCol] = " "
                        if (score < bestScore) {
                            bestScore = score
                        }
                    }
                }
            }
            return bestScore
        }
    }

    private fun checkWinner(board: Array<Array<String>>): Int? {
        var countRowsUser = 0
        var countRowsAI = 0
        var countLeftDiagonalUser = 0
        var countLeftDiagonalAL = 0
        var countRightDiagonalUser = 0
        var countRightDiagonalAI = 0

        board.forEachIndexed { indexRow, cols ->
            if (cols.all { it == PLAYER_SYMBOL })
                return STATUS_PLAYER_WIN
            else if (cols.all { it == BOT_SYMBOL })
                return STATUS_PLAYER_LOSE

            countRowsUser = 0
            countRowsAI = 0

            cols.forEachIndexed { indexCol, cell ->
                if (board[indexCol][indexRow] == PLAYER_SYMBOL)
                    countRowsUser++
                else if (board[indexCol][indexRow] == BOT_SYMBOL)
                    countRowsAI++

                if (indexRow == indexCol && board[indexCol][indexRow] == PLAYER_SYMBOL)
                    countLeftDiagonalUser++
                else if (indexRow == indexCol && board[indexCol][indexRow] == BOT_SYMBOL)
                    countLeftDiagonalAL++

                if (indexRow == 2 - indexCol && board[indexCol][indexRow] == PLAYER_SYMBOL)
                    countRightDiagonalUser++
                else if (indexRow == 2 - indexCol && board[indexCol][indexRow] == BOT_SYMBOL)
                    countRightDiagonalAI++
            }

            if (countRowsUser == 3 || countLeftDiagonalUser == 3 || countRightDiagonalUser == 3)
                return STATUS_PLAYER_WIN
            else if (countRowsAI == 3 || countLeftDiagonalAL == 3 || countRightDiagonalAI == 3)
                return STATUS_PLAYER_LOSE
        }
        board.forEach {
            if (it.find { it == " " } != null)
                return null
        }
        return STATUS_PLAYER_DRAW
    }

    private fun getCurrentSettings(): SettingsActivity.SettingsInfo {
        this.getSharedPreferences("game", MODE_PRIVATE).apply {

            val sound = getInt(PREF_SOUND, 100)
            val level = getInt(PREF_LEVEL, 1)
            val rules = getInt(PREF_RULES, 7)

            return SettingsActivity.SettingsInfo(sound, level, rules)
        }
    }

    data class CellGameField(val row: Int, val col: Int)

    private fun isEmptyField(row: Int, col: Int): Boolean {
        return gameField[row][col] == " "
    }

    private fun makeStep(row: Int, col: Int, symbol: String) {
        gameField[row][col] = symbol

        makeGameFieldUI("$row$col", symbol)
    }

    private fun makeGameFieldUI(position: String, symbol: String) {
        val resId = when (symbol) {
            PLAYER_SYMBOL -> R.drawable.ic_cross
            BOT_SYMBOL -> R.drawable.ic_zero
            else -> return
        }

        when (position) {
            "00" -> binding.cell11.setImageResource(resId)
            "01" -> binding.cell12.setImageResource(resId)
            "02" -> binding.cell13.setImageResource(resId)
            "10" -> binding.cell21.setImageResource(resId)
            "11" -> binding.cell22.setImageResource(resId)
            "12" -> binding.cell23.setImageResource(resId)
            "20" -> binding.cell31.setImageResource(resId)
            "21" -> binding.cell32.setImageResource(resId)
            "22" -> binding.cell33.setImageResource(resId)
        }
    }

    private fun checkGameField(x: Int, y: Int, symbol: String): Boolean {
        var row = 0
        var col = 0
        var leftDiagonal = 0
        var rightDiagonal = 0
        val n = gameField.size

        for (i in 0..2) {
            if (gameField[x][i] == symbol)
                col++
            if (gameField[i][y] == symbol)
                row++
            if (gameField[i][i] == symbol)
                leftDiagonal++
            if (gameField[i][n - i - 1] == symbol)
                rightDiagonal++
        }

        val settings = getCurrentSettings()
        return when (settings.rules) {
            1 -> {
                col == n
            }
            2 -> {
                row == n
            }
            3 -> {
                col == n || row == n
            }
            4 -> {
                leftDiagonal == n || rightDiagonal == n
            }
            5 -> {
                col == n || leftDiagonal == n || rightDiagonal == n
            }
            6 -> {
                row == n || leftDiagonal == n || rightDiagonal == n
            }
            7 -> {
                col == n || row == n || leftDiagonal == n || rightDiagonal == n
            }
            else -> {
                false
            }
        }

    }

    private fun isFilledGame(): Boolean {
        gameField.forEach { strings ->
            if (strings.find { it == " " } != null)
                return false
        }
        return true
    }

    private fun showGameStatus(status: Int) {
        binding.chronometer.stop()

        val dialog = Dialog(this@GameActivity, R.style.Theme_TicTacToy)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.argb(50, 0, 0, 0)))
        dialog.setContentView(R.layout.dialog_popup_status_game)
        dialog.setCancelable(true)

        when (status) {
            STATUS_PLAYER_LOSE -> {
                dialog.findViewById<TextView>(R.id.dialog_text).text = "Вы проиграли!"
                dialog.findViewById<ImageView>(R.id.dialog_image)
                    .setImageResource(R.drawable.status_lose)
            }
            STATUS_PLAYER_WIN -> {
                dialog.findViewById<TextView>(R.id.dialog_text).text = "Вы выиграли!"
                dialog.findViewById<ImageView>(R.id.dialog_image)
                    .setImageResource(R.drawable.status_win)
            }
            STATUS_PLAYER_DRAW -> {
                dialog.findViewById<TextView>(R.id.dialog_text).text = "Ничья!"
                dialog.findViewById<ImageView>(R.id.dialog_image)
                    .setImageResource(R.drawable.status_draw)
            }
        }

        dialog.findViewById<TextView>(R.id.dialog_ok).setOnClickListener {
            dialog.hide()
//            onBackPressed()  // устарел, заменила на
            onBackPressedDispatcher.onBackPressed()
        }
        dialog.show()
    }

    private fun showPopupMenu() {

        binding.chronometer.stop()

        val elapsedMillis = SystemClock.elapsedRealtime() - binding.chronometer.base

        val dialog = Dialog(this@GameActivity, R.style.Theme_TicTacToy)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.argb(50, 0, 0, 0)))
        dialog.setContentView(R.layout.dialog_popup_menu)
        dialog.setCancelable(true)

        dialog.findViewById<TextView>(R.id.dialog_continue).setOnClickListener {
            dialog.hide()
            binding.chronometer.base = SystemClock.elapsedRealtime() - elapsedMillis
            binding.chronometer.start()
        }
        dialog.findViewById<TextView>(R.id.dialog_settings).setOnClickListener {
            dialog.hide()
            val intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, POPUP_MENU)
        }
        dialog.findViewById<TextView>(R.id.dialog_exit).setOnClickListener {
            saveGame(elapsedMillis, convertGameFieldToString())
            dialog.hide()
//            onBackPressed() // устарел, заменила на
            onBackPressedDispatcher.onBackPressed()
        }

        dialog.show()
    }

    companion object {
        const val STATUS_PLAYER_WIN = 1
        const val STATUS_PLAYER_LOSE = 2
        const val STATUS_PLAYER_DRAW = 3
        const val POPUP_MENU = 235

        val scores = hashMapOf(
            Pair(STATUS_PLAYER_WIN, -1.0), Pair(STATUS_PLAYER_LOSE, 1.0), Pair(
                STATUS_PLAYER_DRAW, 0.0
            )
        )
        const val PLAYER_SYMBOL = "X"
        const val BOT_SYMBOL = "0"
    }

}