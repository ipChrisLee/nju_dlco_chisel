package dev_support
import chisel3._
import chisel3.util._

//  Ref: https://github.com/edwardcwang/chisel-multiclock-demo/blob/master/src/main/scala/multiclock_demo/ClockDividerDemo.scala
object ClockDivider {
  def apply(clockIn: Clock, divideBy: Int, resetClk: Option[Bool] = None): Clock = {
    require(divideBy % 2 == 0, "Must divide by an even factor.")
    val newClk = Wire(Clock())
    withClockAndReset(clockIn, resetClk.getOrElse(false.B)) {
      val mxCnt     = divideBy / 2
      val counter   = RegInit(0.U(log2Ceil(mxCnt).W))
      val newClkReg = RegInit(false.B)
      newClkReg := (counter === (mxCnt - 1).U) ^ newClkReg
      counter   := Mux(counter === (mxCnt - 1).U, 0.U, counter + 1.U)
      newClk    := newClkReg.asClock
    }
    newClk
  }
}
