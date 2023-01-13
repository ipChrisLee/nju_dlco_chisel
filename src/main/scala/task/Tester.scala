package task
import chisel3._
import dev_support._

class Tester extends Module {
  val io = IO(new NVBoardIOBundle)
  io.led := 0.U
  io.vga.getElements.foreach(x => x := 0.U)
  io.seg.foreach(x => x := ~0.U(8.W))

  withClockAndReset((!io.ps2.clk).asClock, this.reset.asAsyncReset) {
    val r = RegInit(0.U(4.W))
    r      := r + 1.U
    io.led := r
    assert(r =/= 9.U)
  }
}
