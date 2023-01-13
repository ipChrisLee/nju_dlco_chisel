package task
import chisel3._
import chisel3.util._
import dev_support.{NVBoardIOBundle, SegDefineHex}
import dev_support.PS2
import dev_support.PS2.{ScanCodeToAsciiLUT, Transmitter}

//  https://nju-projectn.github.io/dlco-lecture-note/exp/07.html
class Task07 extends Module {
  val io = IO(new NVBoardIOBundle)
  io.led := 0.U
  io.vga.getElements.foreach(x => x := 0.U)
  io.seg.foreach(x => x := ~0.U(8.W))

  val transmitter = Module(new PS2.Transmitter)
  transmitter.io.ps2.clk := io.ps2.clk
  transmitter.io.ps2.dat := io.ps2.data

  withClockAndReset(transmitter.io.valid.asClock, this.reset.asAsyncReset) {
    val statusRegs = new Bundle {
      val chKey = new Bundle {
        val scanCode = RegInit(0.U(16.W))
        val ascii    = RegInit(0.U(8.W))
        val segOn    = RegInit(false.B)
      }
      val funcKey = new Bundle {
        val shift = RegInit(false.B)
        val ctrl  = RegInit(false.B)
        val caps  = RegInit(false.B)
      }
      val count = RegInit(0.U(8.W))
    }
    val lut = Module(new ScanCodeToAsciiLUT)
    lut.io.scanCode := transmitter.io.scanCode
    lut.io.isUpper  := statusRegs.funcKey.caps || statusRegs.funcKey.shift

    val resetCh = transmitter.io.isBreak && (transmitter.io.scanCode === statusRegs.chKey.scanCode)
    statusRegs.chKey.scanCode := Mux(
      resetCh,
      0.U,
      Mux(lut.io.isAscii, transmitter.io.scanCode, statusRegs.chKey.scanCode)
    )
    statusRegs.chKey.ascii := Mux(
      resetCh,
      0.U,
      Mux(lut.io.isAscii, lut.io.asciiCode, statusRegs.chKey.ascii)
    )
    statusRegs.chKey.segOn := Mux(
      resetCh,
      0.U,
      statusRegs.chKey.segOn || (lut.io.isAscii && !transmitter.io.isBreak)
    )

    statusRegs.funcKey.ctrl  := Mux(lut.io.isCtrl, !transmitter.io.isBreak, statusRegs.funcKey.ctrl)
    statusRegs.funcKey.shift := Mux(lut.io.isShift, !transmitter.io.isBreak, statusRegs.funcKey.shift)
    statusRegs.funcKey.caps := Mux(
      lut.io.isCaps,
      transmitter.io.isBreak ^ statusRegs.funcKey.caps,
//      Mux(transmitter.io.isBreak, !statusRegs.funcKey.caps, statusRegs.funcKey.caps),
      statusRegs.funcKey.caps
    )

    statusRegs.count := statusRegs.count +% (lut.io.isAscii && transmitter.io.isBreak)

    for (i <- 0 to 3) {
      val segDis = Module(new SegDefineHex)
      segDis.io.hasDot := false.B
      segDis.io.num    := statusRegs.chKey.scanCode((i + 1) * 4 - 1, i * 4)
      io.seg(i)        := Mux(statusRegs.chKey.segOn, segDis.io.seg, ~0.U(8.W))
    }
    for (i <- 0 to 1) {
      val segDis = Module(new SegDefineHex)
      segDis.io.hasDot := false.B
      segDis.io.num    := statusRegs.chKey.ascii((i + 1) * 4 - 1, i * 4)
      io.seg(i + 4)    := Mux(statusRegs.chKey.segOn, segDis.io.seg, ~0.U(8.W))
    }
    for (i <- 0 to 1) {
      val segDis = Module(new SegDefineHex)
      segDis.io.hasDot := false.B
      segDis.io.num    := statusRegs.count((i + 1) * 4 - 1, i * 4)
      io.seg(i + 6)    := segDis.io.seg
    }
    io.led := Cat(0.U, statusRegs.funcKey.caps, statusRegs.funcKey.ctrl, statusRegs.funcKey.shift)
  }
}
