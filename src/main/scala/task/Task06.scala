package task
import Chisel.{Cat, MuxLookup, OHToUInt, PriorityEncoderOH, Reverse}
import chisel3._
import dev_support.{NVBoardIOBundle, SegDefineHex}

//  https://nju-projectn.github.io/dlco-lecture-note/exp/06.html
class Task06 extends Module {
  val io = IO(new NVBoardIOBundle)
  io.led := 0.U
  io.vga.getElements.foreach(x => x := 0.U)
  io.seg.foreach(x => x := ~0.U(8.W))

  withClock(io.btn.c.asClock) {
    val state = RegInit(0.U(8.W))
    when(!state.orR) {
      state := 1.U(8.W)
    }.otherwise {
      val newVal = state(4, 2).xorR ^ state(0)
      state  := Cat(newVal, state(7, 1))
      io.led := state
    }
    val seg0Dis = Module(new SegDefineHex)
    io.seg(0)         := seg0Dis.io.seg
    seg0Dis.io.hasDot := false.B
    seg0Dis.io.num    := state(3, 0)

    val seg1Dis = Module(new SegDefineHex)
    io.seg(1)         := seg1Dis.io.seg
    seg1Dis.io.hasDot := false.B
    seg1Dis.io.num    := state(7, 4)
  }
}
