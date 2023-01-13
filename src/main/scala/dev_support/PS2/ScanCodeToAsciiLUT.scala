package dev_support.PS2
import chisel3._
import chisel3.util._

class ScanCodeToAsciiLUT extends Module {
  val io = IO {
    new Bundle {
      val scanCode  = Input(UInt(16.W))
      val isUpper   = Input(Bool())
      val isAscii   = Output(Bool())
      val asciiCode = Output(UInt(8.W))
      val isCtrl    = Output(Bool())
      val isShift   = Output(Bool())
      val isCaps    = Output(Bool())
    }
  }
  val lut = Array(
    0x0e -> ('~', '`'),
    0x16 -> ('1', '!'),
    0x1e -> ('2', '@'),
    0x26 -> ('3', '#'),
    0x25 -> ('4', '$'),
    0x2e -> ('5', '%'),
    0x36 -> ('6', '^'),
    0x3d -> ('7', '&'),
    0x3e -> ('8', '*'),
    0x46 -> ('9', '('),
    0x45 -> ('0', ')'),
    0x4e -> ('-', '_'),
    0x55 -> ('=', '+'),
    0x5d -> ('\\', '|'),
    0x15 -> ('q', 'Q'),
    0x1d -> ('w', 'W'),
    0x24 -> ('e', 'E'),
    0x2d -> ('r', 'R'),
    0x2c -> ('t', 'T'),
    0x35 -> ('y', 'Y'),
    0x3c -> ('u', 'U'),
    0x43 -> ('i', 'I'),
    0x44 -> ('o', 'O'),
    0x4d -> ('p', 'P'),
    0x54 -> ('[', '{'),
    0x5b -> (']', '}'),
    0x1c -> ('a', 'A'),
    0x1b -> ('s', 'S'),
    0x23 -> ('d', 'D'),
    0x2b -> ('f', 'F'),
    0x34 -> ('g', 'G'),
    0x33 -> ('h', 'H'),
    0x3b -> ('j', 'J'),
    0x42 -> ('k', 'K'),
    0x4b -> ('l', 'L'),
    0x4c -> (';', ':'),
    0x52 -> ('\'', '"'),
    0x1a -> ('z', 'Z'),
    0x22 -> ('x', 'X'),
    0x21 -> ('c', 'C'),
    0x2a -> ('v', 'V'),
    0x32 -> ('b', 'B'),
    0x31 -> ('n', 'N'),
    0x3a -> ('m', 'M'),
    0x41 -> (',', '<'),
    0x49 -> ('.', '>'),
    0x4a -> ('/', '?'),
    0x0d -> ('\t', '\t'),
    0x66 -> ('\b', '\b'),
    0x29 -> (' ', ' '),
    0x5a -> ('\n', '\n')
  )

  io.asciiCode :=
    Mux(
      io.isUpper,
      MuxLookup(
        io.scanCode(7, 0),
        0.U(8.W),
        lut.collect { case p => (p._1.U(8.W), p._2._2.U(8.W)) }.toIndexedSeq
      ),
      MuxLookup(
        io.scanCode(7, 0),
        0.U(8.W),
        lut.collect { case p => (p._1.U(8.W), p._2._1.U(8.W)) }.toIndexedSeq
      )
    )
  io.isAscii := (!io.scanCode(15, 8).orR) && io.asciiCode.orR

  io.isCtrl  := io.scanCode === 0x14.U || io.scanCode === 0xe014.U
  io.isShift := io.scanCode === 0x12.U || io.scanCode === 0x59.U
  io.isCaps  := io.scanCode === 0x58.U
}
