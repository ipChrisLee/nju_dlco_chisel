package task
import chisel3._
import chisel3.util._
import dev_support.PS2.{ScanCodeInfo, ScanCodeToAsciiLUT, Transmitter}
import dev_support._

class Task09BK extends Module {
  val io = IO(new NVBoardIOBundle)
  io.seg.foreach { x => x := 0.U }
  io.led := 0.U

  val width  = 640
  val height = 480

  //  keyboard
  val keyboard = RegInit({
    val b = Wire(new Bundle {
      val capsOn = Bool()
    })
    b.capsOn := false.B
    b
  })
  val keyboardLUT         = Module(new ScanCodeToAsciiLUT)
  val keyboardTransmitter = Module(new Transmitter)
  keyboardTransmitter.io.ps2.clk := io.ps2.clk
  keyboardTransmitter.io.ps2.dat := io.ps2.data
  keyboardLUT.io.scanCode        := keyboardTransmitter.io.scanCode
  keyboardLUT.io.isUpper         := false.B

  val deleteThis, dirR, dirC =
    withClockAndReset(keyboardTransmitter.io.valid.asClock, reset.asAsyncReset) {

      (0.U(1.W), 0.U(1.W), 0.U(1.W))
    }

  //  cursor
  val cursor =
    withClockAndReset(keyboardTransmitter.io.valid.asClock, reset.asAsyncReset) {
      RegInit({
        val b = Wire(new Bundle {
          val r = UInt(log2Ceil(height).W)
          val c = UInt(log2Ceil(width).W)
        })
        b.r := 1.U
        b.c := 0.U
        b
      })
    }

  //  vga scan display mat
  val displayMat = RegInit(VecInit.fill(30 * 70)(0.U(8.W)))

  val asciiFontMem = Mem(256 * 16, UInt(9.W))
  chisel3.util.experimental.loadMemoryFromFileInline(asciiFontMem, "resource/vga_font.txt")

  val vgaCtrl = Module(new VgaCtrl)
  vgaCtrl.io.i.clk := clock
  vgaCtrl.io.i.rst := reset
  io.vga.clk       := clock.asBool
  io.vga.blankN    := vgaCtrl.io.o.valid
  io.vga.vSync     := vgaCtrl.io.o.vSync
  io.vga.hSync     := vgaCtrl.io.o.hSync
  io.vga.r         := vgaCtrl.io.o.vgaR
  io.vga.g         := vgaCtrl.io.o.vgaG
  io.vga.b         := vgaCtrl.io.o.vgaB

  val row   = vgaCtrl.io.o.vAddr / 16.U(10.W)
  val col   = vgaCtrl.io.o.hAddr / 9.U(10.W)
  val rBias = vgaCtrl.io.o.vAddr % 16.U
  val cBias = vgaCtrl.io.o.hAddr % 9.U

  //  display
  val cursorBlinkMx = 1
  val cursorCounter =
    withClockAndReset(vgaCtrl.io.o.vSync.asClock, reset.asAsyncReset) {
      val cursorCounter = Counter(cursorBlinkMx * 2)
      cursorCounter.inc()
      cursorCounter
    }
  val isWhite =
    asciiFontMem(displayMat(row * 70.U + col) * 16.U + rBias)(cBias) ^
      (cursor.r === row && cursor.c === col && cursorCounter.value < cursorBlinkMx.U)

//  assert(cursor.r =/= row || cursor.c =/= col)

  vgaCtrl.io.i.vgaData := Fill(24, isWhite)

}
