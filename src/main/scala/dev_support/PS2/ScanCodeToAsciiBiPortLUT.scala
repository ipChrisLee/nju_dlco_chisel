package dev_support.PS2

import chisel3._
import chisel3.util._

class ScanCodeToAsciiBiPortLUT extends Module {
  val io = IO {
    new Bundle {
      val scanCode       = Input(UInt(16.W))
      val isAscii        = Output(Bool())
      val asciiCode      = Output(UInt(8.W))
      val upperAsciiCode = Output(UInt(8.W))
      val isLetter       = Output(Bool())
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
    0x29 -> (' ', ' ')
  )

  val letters  = ('a' to 'z').toSet ++ ('A' to 'Z').toSet
  val isLetter = VecInit(letters.collect { ch => io.asciiCode === ch.toByte.U }.toIndexedSeq)
  io.isLetter := isLetter.asUInt.orR

  io.asciiCode :=
    MuxLookup(
      io.scanCode(7, 0),
      0.U(8.W),
      lut.collect { case p => (p._1.U(8.W), p._2._1.U(8.W)) }.toIndexedSeq
    )
  io.upperAsciiCode :=
    MuxLookup(
      io.scanCode(7, 0),
      0.U(8.W),
      lut.collect { case p => (p._1.U(8.W), p._2._2.U(8.W)) }.toIndexedSeq
    )
  io.isAscii := io.upperAsciiCode.orR || io.asciiCode.orR
}
