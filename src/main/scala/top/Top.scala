package top
import chisel3._
import chisel3.util.{Cat, MuxLookup}
import dev_support.NVBoardIOBundle
import task.{Task01, Task02, Task03, Task06, Task07, Task08, Tester}

class Top extends Module {
  val io  = IO(new NVBoardIOBundle)
  val ker = Module(new Tester);
  io <> ker.io
}
