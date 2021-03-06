package com.example.androiddevchallenge

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    // -----------------------------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------------------------

    companion object {
        private const val hoursToSeconds = 3600
        private const val minutesToSeconds = 60
        private const val secondsToMilliseconds = 1_000L
    }

    // -----------------------------------------------------------------------------------------
    // Properties
    // -----------------------------------------------------------------------------------------

    private var countDownTimer: CountDownTimer? = null
    private val _inputNumbers = MutableStateFlow(value = 0)

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState>
        get() = _state

    // -----------------------------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------------------------

    init {
        viewModelScope.launch {
            _inputNumbers.collect { time ->
                val seconds = time % 100
                val hoursAndMinutes = time / 100
                val minutes = hoursAndMinutes % 100
                val hours = hoursAndMinutes / 100
                _state.value = TimerState(
                    countDownTimeInSec = hours * hoursToSeconds + minutes * minutesToSeconds + seconds,
                    countDownTimeTriple = Triple(hours, minutes, seconds),
                    isTimeValid = time > 0
                )
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Public functions
    // -----------------------------------------------------------------------------------------

    fun addTime(time: Int) = viewModelScope.launch {
        if (_inputNumbers.value.toString().length < 6) {
            val newTimeValue = _inputNumbers.value.times(10).plus(time)
            _inputNumbers.value = newTimeValue
        }
    }

    fun subtractTime() = viewModelScope.launch {
        _inputNumbers.value = _inputNumbers.value.div(10)
    }

    fun clearTime() = viewModelScope.launch {
        _inputNumbers.value = 0
    }

    fun startCountingDownTime() = viewModelScope.launch {
        _state.value = _state.value.copy(screen = Screen.CountDown)
        val countDownTime = _state.value.countDownTimeInSec
        countDownTimer = object :
            CountDownTimer(countDownTime.times(secondsToMilliseconds), secondsToMilliseconds) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsedTimeInSec = millisUntilFinished / secondsToMilliseconds
                _state.value = _state.value.copy(
                    elapsedTimeInSec = elapsedTimeInSec.toInt(),
                    elapsedTimeTriple = getElapsedTimeTriple(elapsedTimeInSec.toInt())
                )
            }

            override fun onFinish() {
                Timber.d("## Count down finished...")
                _state.value = _state.value.copy(isCountDownCompleted = true)
            }
        }
        Timber.d("## Starting count down")
        countDownTimer?.start()
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }

    private fun getElapsedTimeTriple(elapsedTimeInSec: Int): Triple<Int, Int, Int> {
        val hours = elapsedTimeInSec / 3600
        val minutes = (elapsedTimeInSec % 3600) / 60
        val seconds = elapsedTimeInSec % 60
        return Triple(hours, minutes, seconds)
    }
}

// -----------------------------------------------------------------------------------------
// View State
// -----------------------------------------------------------------------------------------

data class TimerState(
    val screen: Screen = Screen.Editing,
    val countDownTimeInSec: Int = 0,
    val elapsedTimeInSec: Int = 0,
    val countDownTimeTriple: Triple<Int, Int, Int> = Triple(0, 0, 0), // hours, minutes, seconds
    val elapsedTimeTriple: Triple<Int, Int, Int> = Triple(0, 0, 0), // hours, minutes, seconds
    val isTimeValid: Boolean = false, // input time greater than zero
    val isCountDownCompleted: Boolean = false
)

sealed class Screen {
    object Editing : Screen()
    object CountDown : Screen()
}
