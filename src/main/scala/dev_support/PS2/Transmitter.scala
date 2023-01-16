package dev_support.PS2
import chisel3._
import chisel3.util._

class Transmitter extends Module {
  val io = IO(new Bundle {
    val ps2 = new Bundle {
      val clk = Input(Bool())
      val dat = Input(Bool())
    }
    val isBreak  = Output(Bool())
    val scanCode = Output(UInt(16.W))
    val valid    = Output(Bool())
  })

  withClockAndReset((!io.ps2.clk).asClock, this.reset.asAsyncReset) {
    val count     = RegInit(0.U(4.W))
    val receiving = RegInit(false.B)
    val fin       = RegInit(false.B)
    val buffer    = RegInit(0.U(8.W))
    when(!receiving && count === 0.U && !fin) {
      assert(io.ps2.dat === false.B)
      receiving := true.B
      count     := 0.U
      fin       := false.B
      buffer    := 0.U
    }.elsewhen(receiving && count < 8.U && !fin) {
      receiving := true.B
      count     := count + 1.U
      fin       := false.B
      buffer    := Cat(io.ps2.dat, buffer >> 1)
    }.elsewhen(receiving && count === 8.U && !fin) {
      assert(buffer.xorR ^ io.ps2.dat === true.B) //  TODO: what to do if assert failed
      receiving := true.B
      count     := 8.U
      fin       := true.B
    }.elsewhen(receiving && count === 8.U && fin) {
      assert(io.ps2.dat === true.B)
      receiving := false.B
      count     := 0.U
      fin       := false.B
    }.otherwise {
      assert(false.B)
    }

    val gotF0 = RegInit(false.B)
    gotF0 := Mux(
      fin,
      Mux(buffer === 0xe0.U, gotF0, buffer === 0xf0.U),
      gotF0
    )
    val gotE0 = RegInit(false.B)
    gotE0 := Mux(
      fin,
      Mux(buffer === 0xf0.U, gotE0, buffer === 0xe0.U),
      gotE0
    )

    io.valid    := fin && buffer =/= 0xf0.U && buffer =/= 0xe0.U
    io.scanCode := Cat(Fill(3, gotE0), 0.U(1.W), 0.U(4.W), buffer)
    io.isBreak  := gotF0
  }
}
