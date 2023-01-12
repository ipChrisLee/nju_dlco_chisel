package task
import Chisel.{Cat, MuxLookup, OHToUInt, PriorityEncoderOH, Reverse}
import chisel3._
import dev_support.{NVBoardIOBundle, SegDefineDec}

//  https://nju-projectn.github.io/dlco-lecture-note/exp/03.html
class Task03 extends Module {
  val io = IO(new NVBoardIOBundle)
  io.led := 0.U
  io.vga.getElements.foreach(x => x := 0.U)
  io.seg.foreach(x => x := ~0.U(8.W))

  val operandL = io.sw(7, 4).asSInt
  val operandR = io.sw(3, 0).asSInt
  val out = MuxLookup(
    io.sw(10, 8),
    0.S(4.W),
    Array(
      "b000" -> (operandL +% operandR),
      "b001" -> (operandL -% operandR),
      "b010" -> (~operandL),
      "b011" -> (operandL & operandR),
      "b100" -> (operandL | operandR),
      "b101" -> (operandL ^ operandR),
      "b110" -> (operandL < operandR).zext,
      "b111" -> (operandL === operandR).zext
    ).collect({ case s => (s._1.U(3.W), s._2) }).toIndexedSeq
  )
  io.led := Cat(0.U, out)
}
