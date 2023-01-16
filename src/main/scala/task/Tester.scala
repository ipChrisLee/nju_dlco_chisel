package task
import chisel3._
import chisel3.util._
import dev_support._

class Tester extends Module {
  val io = IO(new NVBoardIOBundle)
  io.led := 0.U
  io.vga.getElements.foreach(x => x := 0.U)
  io.seg.foreach(x => x := ~0.U(8.W))

  withClockAndReset(io.sw(0).asClock, reset) {
    val rf = Reg(new Bundle {
      val a0 = UInt(8.W)
    })
    rf.a0 := ~rf.a0
    val rfi = RegInit({
      val b = Wire(new Bundle {
        val a0 = UInt(8.W)
      })
      b.a0 := 0.U
      b
    })
    rfi.a0 := ~rfi.a0
    io.led := Cat(0.U, rf.a0, rfi.a0)
  }
}
