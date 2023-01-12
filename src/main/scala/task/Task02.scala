package task
import Chisel.{Cat, MuxLookup, OHToUInt, PriorityEncoderOH, Reverse}
import chisel3._
import dev_support.{NVBoardIOBundle, SegDefineDec}

//  https://nju-projectn.github.io/dlco-lecture-note/exp/02.html
class Task02 extends Module {
  val io = IO(new NVBoardIOBundle)
  io.led := 0.U
  io.vga.getElements.foreach(x => x := 0.U)
  io.seg.foreach(x => x := ~0.U(8.W))

  val num = OHToUInt(Reverse(PriorityEncoderOH(Reverse(io.sw(7, 0)))), 8)
  io.led := Cat(0.U, io.sw(8), 0.U(1.W), num(2, 0))

  val segDefine = Module(new SegDefineDec)
  io.seg(0)           := segDefine.io.seg
  segDefine.io.hasDot := false.B
  segDefine.io.num    := num
}
