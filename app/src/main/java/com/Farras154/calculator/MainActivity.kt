package com.Farras154.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Farras154.calculator.ui.theme.CalculatorTheme
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculatorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CalculatorScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
    }
}

// --- State & Logic ---
private enum class Op { ADD, SUB, MUL, DIV, NONE }

private data class CalcState(
    val display: String = "0",
    val accumulator: BigDecimal? = null,
    val pending: Op = Op.NONE,
    val justEvaluated: Boolean = false
)

@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    var state by remember { mutableStateOf(CalcState()) }

    fun inputDigit(d: String) {
        state = if (state.justEvaluated) {
            state.copy(display = if (d == ".") "0." else d, justEvaluated = false)
        } else {
            val current = state.display
            val next = when {
                d == "." && current.contains(".") -> current
                current == "0" && d != "." -> d
                else -> current + d
            }
            state.copy(display = next)
        }
    }

    fun setOp(op: Op) {
        val currentNumber = state.display.toBigDecimalOrNull()
        if (currentNumber != null) {
            // snapshot dulu
            val acc0 = state.accumulator
            val pending0 = state.pending
            val justEval0 = state.justEvaluated

            val acc = when {
                acc0 == null -> currentNumber
                pending0 != Op.NONE && !justEval0 -> eval(acc0, currentNumber, pending0)
                else -> acc0
            }
            state = state.copy(
                display = "0",
                accumulator = acc,
                pending = op,
                justEvaluated = false
            )
        } else {
            state = CalcState()
        }
    }

    fun equalsNow() {
        val right = state.display.toBigDecimalOrNull() ?: return

        // snapshot dulu
        val left = state.accumulator
        val pending0 = state.pending

        if (left != null && pending0 != Op.NONE) {
            val res = eval(left, right, pending0)
            state = CalcState(
                display = res.stripTrailingZerosSafe(),
                accumulator = null,
                pending = Op.NONE,
                justEvaluated = true
            )
        } else {
            state = state.copy(justEvaluated = true)
        }
    }


    fun toggleSign() {
        val cur = state.display
        state = state.copy(
            display = if (cur.startsWith("-")) cur.removePrefix("-") else if (cur != "0") "-$cur" else cur
        )
    }

    fun percent() {
        val bd = state.display.toBigDecimalOrNull() ?: return
        state = state.copy(display = bd.divide(BigDecimal(100), mc).stripTrailingZerosSafe())
    }

    fun deleteOne() {
        val cur = state.display
        val next = when {
            state.justEvaluated -> "0"
            cur.length <= 1 || (cur.startsWith("-") && cur.length == 2) -> "0"
            else -> cur.dropLast(1)
        }
        state = state.copy(display = next, justEvaluated = false)
    }

    fun clearAll() { state = CalcState() }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = state.display,
                fontSize = 48.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                lineHeight = 52.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Keypad
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FuncButton("AC", Modifier.weight(1f)) { clearAll() }
                FuncButton("DEL", Modifier.weight(1f)) { deleteOne() }
                FuncButton("%", Modifier.weight(1f)) { percent() }
                OpButton("÷", Modifier.weight(1f)) { setOp(Op.DIV) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumButton("7", Modifier.weight(1f)) { inputDigit("7") }
                NumButton("8", Modifier.weight(1f)) { inputDigit("8") }
                NumButton("9", Modifier.weight(1f)) { inputDigit("9") }
                OpButton("×", Modifier.weight(1f)) { setOp(Op.MUL) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumButton("4", Modifier.weight(1f)) { inputDigit("4") }
                NumButton("5", Modifier.weight(1f)) { inputDigit("5") }
                NumButton("6", Modifier.weight(1f)) { inputDigit("6") }
                OpButton("−", Modifier.weight(1f)) { setOp(Op.SUB) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumButton("1", Modifier.weight(1f)) { inputDigit("1") }
                NumButton("2", Modifier.weight(1f)) { inputDigit("2") }
                NumButton("3", Modifier.weight(1f)) { inputDigit("3") }
                OpButton("+", Modifier.weight(1f)) { setOp(Op.ADD) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FuncButton("+/−", Modifier.weight(1f)) { toggleSign() }
                NumButton("0", Modifier.weight(1f)) { inputDigit("0") }
                NumButton(".", Modifier.weight(1f)) { inputDigit(".") }
                EqualsButton("=", Modifier.weight(1f)) { equalsNow() }
            }
        }
    }
}

// --- Buttons ---
@Composable
private fun NumButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun OpButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun FuncButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EqualsButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
    ) {
        Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiary)
    }
}

// --- Math helpers ---
private val mc = MathContext(16, RoundingMode.HALF_UP)

private fun eval(a: BigDecimal, b: BigDecimal, op: Op): BigDecimal = when (op) {
    Op.ADD -> a.add(b, mc)
    Op.SUB -> a.subtract(b, mc)
    Op.MUL -> a.multiply(b, mc)
    Op.DIV -> if (b.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO
    else a.divide(b, 10, RoundingMode.HALF_UP).stripTrailingZeros()
    Op.NONE -> b
}

private fun BigDecimal.stripTrailingZerosSafe(): String {
    return try {
        this.stripTrailingZeros().toPlainString()
    } catch (_: Exception) {
        this.toPlainString()
    }
}

@Preview(showBackground = true)
@Composable
fun CalculatorPreview() {
    CalculatorTheme {
        CalculatorScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
    }
}