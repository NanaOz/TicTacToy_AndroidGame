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

    private lateinit var settingsInfo: SettingsActivity.SettingsInfo

    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGameBinding.inflate(layoutInflater)

        binding.toPopupMenu.setOnClickListener {
            showPopupMenu()
        }

        binding.toGameClose.setOnClickListener {
            onBackPressed()
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

        val time = intent.getLongExtra(MainActivity.EXTRA_TIME, 0)
        val gameField = intent.getStringExtra(MainActivity.EXTRA_GAME_FIELD)

        if (gameField != null && time != 0L && gameField != "") {
            restartGame(time, gameField)
        } else {
            initGameField()
        }

        settingsInfo = getSettingsInfo()

        mediaPlayer = MediaPlayer.create(this, R.raw.mus)
        mediaPlayer.isLooping = true
        setVolumeMediaPlayer(settingsInfo.soundValue)

        mediaPlayer.start()

        binding.chronometer.start()

    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.release()
    }

    private fun setVolumeMediaPlayer(soundValue: Int) {
        val volume = soundValue / 100.0
        mediaPlayer.setVolume(volume.toFloat(), volume.toFloat())
    }

    private fun initGameField() {
        gameField = Array(3) { Array(3) { " " } }
    }

    private fun makeStep(row: Int, col: Int, symbol: String) {
        gameField[row][col] = symbol

        makeStepUI("$row$col", symbol)
    }

    private fun makeStepUI(position: String, symbol: String) {
        val resId = when (symbol) {
            "X" -> R.drawable.ic_cross
            "0" -> R.drawable.ic_zero
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

    private fun makeStepOfUser(row: Int, col: Int) {
        if (isEmptyField(row, col)) {
            makeStep(row, col, "X")

            val status = checkGameField(row, col, "X")
            if (status.status) {
                showGameStatus(STATUS_PLAYER_WIN)
                return
            }

            if (!isFilledGame()) {
                val resultCell = makeStepOfAI()

                val statusAI = checkGameField(resultCell.row, resultCell.col, "0")
                if (statusAI.status) {
                    showGameStatus(STATUS_PLAYER_LOSE)
                    return
                }

                if (isFilledGame()) {
                    showGameStatus(STATUS_PLAYER_DRAW)
                    return
                }
            } else {
                showGameStatus(STATUS_PLAYER_DRAW)
                return
            }

        } else {
            Toast.makeText(this, "Поле уже занято", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isEmptyField(row: Int, col: Int): Boolean {
        return gameField[row][col] == " "
    }

    private fun makeStepOfAI() : CellGameField {
        return when (settingsInfo.lvl) {
            0 -> makeStepOfAIEasyLvl()
            1 -> makeStepOfAIMediumLvl()
            2 -> makeStepOfAIHardLvl()
            else -> CellGameField(0, 0)
        }

    }

    data class CellGameField(val row: Int, val col: Int)

    private fun makeStepOfAIHardLvl() :CellGameField {
        var bestScore = Double.NEGATIVE_INFINITY
        var move = CellGameField(0, 0)

        var board = gameField.map { it.clone() }.toTypedArray()

        board.forEachIndexed { indexRow, cols ->
            cols.forEachIndexed { indexCol, cell ->
                if (board[indexRow][indexCol] == " ") {
                    board[indexRow][indexCol] = "0"
                    val score = minimax(board, false)
                    board[indexRow][indexCol] = " "
                    if (score > bestScore) {
                        bestScore = score
                        move = CellGameField(indexRow, indexCol)
                    }
                }
            }
        }
        makeStep(move.row, move.col, "0")

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
                        board[indexRow][indexCol] = "0"
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
                        board[indexRow][indexCol] = "X"
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
        var countRowsHu = 0
        var countRowsAI = 0
        var countLRHu = 0
        var countLRAI = 0
        var countRDHu = 0
        var countRDAI = 0

        board.forEachIndexed { indexRow, cols ->
            if (cols.all { it == "X" })
                return STATUS_PLAYER_WIN
            else if (cols.all { it == "0" })
                return STATUS_PLAYER_LOSE
            countRowsHu = 0
            countRowsAI = 0

            cols.forEachIndexed { indexCol, cell ->
                if (board[indexCol][indexRow] == "X")
                    countRowsHu++
                else if (board[indexCol][indexRow] == "0")
                    countRowsAI++

                if (indexRow == indexCol && board[indexCol][indexRow] == "X")
                    countLRHu++
                else if (indexRow == indexCol && board[indexCol][indexRow] == "0")
                    countLRAI++

                if (indexRow == 2 - indexCol && board[indexCol][indexRow] == "X")
                    countRDHu++
                else if (indexRow == 2 - indexCol && board[indexCol][indexRow] == "0")
                    countRDAI++
            }

            if (countRowsHu == 3 || countLRHu == 3 || countRDHu == 3)
                return STATUS_PLAYER_WIN
            else if (countRowsAI == 3 || countLRAI == 3 || countRDAI == 3)
                return STATUS_PLAYER_LOSE
        }
        board.forEach {
            if (it.find { it == " " } != null)
                return null
        }
        return STATUS_PLAYER_DRAW
    }


    private fun makeStepOfAIMediumLvl() :CellGameField {
        var bestScore = Double.NEGATIVE_INFINITY
        var move = CellGameField(0, 0)

        var board = gameField.map { it.clone() }.toTypedArray()

        board.forEachIndexed { indexRow, cols ->
            cols.forEachIndexed { indexCol, cell ->
                if (board[indexRow][indexCol] == " ") {
                    board[indexRow][indexCol] = "0"
                    val score = minimax(board, false)
                    board[indexRow][indexCol] = " "
                    if (score > bestScore) {
                        bestScore = score
                        move = CellGameField(indexRow, indexCol)
                    }
                }
            }
        }
        makeStep(move.row, move.col, "0")

        return move
    }

    private fun makeStepOfAIEasyLvl() : CellGameField{
        var randRow = 0
        var randCol = 0

        do {
            randRow = (0..2).random()
            randCol = (0..2).random()
        } while (!isEmptyField(randRow, randCol))

        makeStep(randRow, randCol, "0")

        return CellGameField(randRow, randCol)
    }

    private fun checkGameField(x: Int, y: Int, symbol: String): StatusInfo {
        var row = 0
        var col = 0
        var leftDiagonal = 0
        var rightDiagonal = 0
        var n = gameField.size

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

        return when (settingsInfo.rules) {
            1 -> {
                if (col == n)
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")

            }
            2 -> {
                if (row == n)
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")

            }
            3 -> {
                if (col == n || row == n)
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }
            4 -> {
                if (leftDiagonal == n || rightDiagonal == n)
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }
            5 -> {
                if (col == n || leftDiagonal == n || rightDiagonal == n)
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }
            6 -> {
                if (row == n || leftDiagonal == n || rightDiagonal == n)
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }
            7 -> {
                if (col == n || row == n || leftDiagonal == n || rightDiagonal == n)
                    StatusInfo(true, symbol)
                else
                    StatusInfo(false, "")
            }
            else -> StatusInfo(false, "")
        }


    }

    data class StatusInfo(val status: Boolean, val side: String)

    private fun showGameStatus(status: Int) {
        val dialog = Dialog(this, R.style.Theme_TicTacToy)
        with(dialog) {
            window?.setBackgroundDrawable(ColorDrawable(Color.argb(50, 0, 0, 0)))
            setContentView(R.layout.dialog_popup_status_game)
            setCancelable(true)
        }

        val image = dialog.findViewById<ImageView>(R.id.dialog_image)
        val text = dialog.findViewById<TextView>(R.id.dialog_text)
        val button = dialog.findViewById<TextView>(R.id.dialog_ok)

        button.setOnClickListener {
            onBackPressed()
        }

        when (status) {
            STATUS_PLAYER_WIN -> {
                image.setImageResource(R.drawable.ic_win)
                text.text = getString(R.string.dialog_status_win)
            }
            STATUS_PLAYER_LOSE -> {
                image.setImageResource(R.drawable.ic_lose)
                text.text = getString(R.string.dialog_status_lose)
            }
            STATUS_PLAYER_DRAW -> {
                image.setImageResource(R.drawable.ic_draw)
                text.text = getString(R.string.dialog_status_draw)
            }
        }

        dialog.show()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_POPUP_MENU) {
            if (resultCode == RESULT_OK) {
                settingsInfo = getSettingsInfo()

                mediaPlayer = MediaPlayer.create(this, R.raw.mus)
                mediaPlayer.isLooping = true
                setVolumeMediaPlayer(settingsInfo.soundValue)

                mediaPlayer.start()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }

    }

    private fun showPopupMenu() {
        val dialog = Dialog(this, R.style.Theme_TicTacToy)
        with(dialog) {
            window?.setBackgroundDrawable(ColorDrawable(Color.argb(50, 0, 0, 0)))
            setContentView(R.layout.dialog_popup_menu)
            setCancelable(true)
        }

        val toContinue = dialog.findViewById<ImageView>(R.id.dialog_continue)
        val toSettings = dialog.findViewById<TextView>(R.id.dialog_settings)
        val toExit = dialog.findViewById<TextView>(R.id.dialog_exit)

        toContinue.setOnClickListener {
            dialog.hide()
        }

        toSettings.setOnClickListener {
            dialog.hide()
            val intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, REQUEST_POPUP_MENU)


        }

        toExit.setOnClickListener {
            val elapsedMills = SystemClock.elapsedRealtime() - binding.chronometer.base
            val gameField = convertGameFieldToString(gameField)
            saveGame(elapsedMills, gameField)
            dialog.dismiss()
            onBackPressed()
        }

        dialog.show()
    }

    private fun isFilledGame(): Boolean {
        gameField.forEach { strings ->
            if (strings.find { it == " " } != null)
                return false
        }
        return true
    }

    private fun convertGameFieldToString(gameField: Array<Array<String>>): String {
        val tmpArray = arrayListOf<String>()
        gameField.forEach { tmpArray.add(it.joinToString(";")) }
        return tmpArray.joinToString("\n")
    }

    private fun saveGame(time: Long, gameField: String) {
        with(getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE).edit()) {
            putLong(PREF_TIME, time)
            putString(PREF_GAME_FIELD, gameField)
            apply()
        }
    }

    private fun restartGame(time: Long, gameField: String) {
        binding.chronometer.base = SystemClock.elapsedRealtime() - time

        this.gameField = arrayOf()

        val rows = gameField.split("\n")
        rows.forEach {
            val columns = it.split(";")
            this.gameField += columns.toTypedArray()
        }

        this.gameField.forEachIndexed { indexRow, strings ->
            strings.forEachIndexed { indexCol, s ->
                makeStep(indexRow, indexCol, this.gameField[indexRow][indexCol])
            }
        }
    }

    private fun getSettingsInfo(): SettingsActivity.SettingsInfo {
        with(getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)) {
            val soundValue = getInt(SettingsActivity.PREF_SOUND_VALUE, 50)
            val lvl = getInt(SettingsActivity.PREF_LVL, 0)
            val rules = getInt(SettingsActivity.PREF_RULES, 7)

            return SettingsActivity.SettingsInfo(soundValue, lvl, rules)
        }
    }

    companion object {
        const val STATUS_PLAYER_WIN = 1
        const val STATUS_PLAYER_LOSE = 2
        const val STATUS_PLAYER_DRAW = 3
        const val PREF_TIME = "pref_time"
        const val PREF_GAME_FIELD = "pref_game_field"
        const val REQUEST_POPUP_MENU = 123
        val scores = hashMapOf(
            Pair(STATUS_PLAYER_WIN, -1.0), Pair(STATUS_PLAYER_LOSE, 1.0), Pair(
                STATUS_PLAYER_DRAW, 0
            )
        )
    }
}