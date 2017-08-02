///** soar
//  *
//  * Copyright (c) 2017 Hugo Firth
//  * Email: <me@hugofirth.com/>
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at:
//  *
//  * http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */
//package uk.ac.ncl.la.soar.glance.web.client.component
//
//import cats._
//import cats.data.{State, StateT}
//import cats.implicits._
//import monix.eval.Task
//import monix.cats._
//import japgolly.scalajs.react._
//import japgolly.scalajs.react.CatsReact._
//import japgolly.scalajs.react.extra.StateSnapshot
//import japgolly.scalajs.react.raw.SyntheticDragEvent
//import japgolly.scalajs.react.{Callback, StateAccessor}
//import japgolly.scalajs.react.vdom.html_<^._
//import scalajs.js
//import org.scalajs.dom
//
///**
//  * React Drag and Drop list component lifted from https://gist.github.com/japgolly/4bcfdcac208bbb7d925e and ported to
//  * Cats.
//  */
//object DragNDrop {
//
//  def move[A: Eq](from: A, to: A)(l: List[A]): List[A] = {
//    println(s"DragNDrop Move: $from -> $to")
//    l.find(from === _) match {
//      case None => l
//      case Some(f) =>
//        var removedYet = false
//        l.flatMap { i =>
//          var x = if(from === i) { removedYet = true; List.empty[A] } else { List(i) }
//          if(to === i) { x = if(removedYet) x :+ f else f :: x }
//          x
//        }
//    }
//  }
//
//  object Parent {
//    type PState[A] = Option[(A, Option[A])] //src, target
//
//    def initialState[A]: PState[A] = None
//
//    private def setStateDrop[A](tgt: Option[A], st: PState[A]): PState[A] = st.map { case (src, _) => (src, tgt) }
//
//    def dragEnd[A] = none[PState[A]]
//    def dragStart[A](a: A) = (a, None).some
//
//    def dragOver[A](a: A, st: PState[A]) = setStateDrop(a.some, st)
//    def dragLeave[A](st: PState[A]) = setStateDrop(none[A], st)
//
//    def cProps[A: Eq](st: PState[A], a: A, move: (A, A) => Callback) = {
//      Child.CProps[A](
//        st match {
//          case Some((_, Some(d))) => a === d
//          case _ => false
//        },
//        {a: A => Callback(dragStart(a))},
//        {(a: A, st: PState[A]) => Callback(dragOver(a, st))},
//        {st: PState[A] => Callback(dragLeave(st))},
//        Callback(dragEnd),
//        st match {
//          case Some((from, Some(to))) => move(from, to)
//          case _ => Callback.empty
//        }
//      )
//    }
//  }
//
//  object Child {
//
//    case class CProps[A](isDraggedOver: Boolean,
//                         onDragStart: A => Callback,
//                         onDragOver: (A, Parent.PState[A]) => Callback,
//                         onDragLeave: Parent.PState[A] => Callback,
//                         onDragEnd: Callback,
//                         onMove: Callback)
//
//    type CState = Boolean
//
//    type StateCB[A] = StateT[CallbackTo, CState, A]
//
//    def initialState: CState = false
//
//    def dragStart[A](a: A, p: CProps[A]): SyntheticDragEvent[dom.Node] => CallbackTo[CState] = { e =>
//      p.onDragStart(a) >> CallbackTo {
//        e.dataTransfer.setData("text", "managed")
//        true
//      }
//    }
//
//    def dragEnd[A](p: CProps[A]): CallbackTo[CState] = p.onDragEnd >> CallbackTo(false)
//
//    def dragOver[A](a: A, p: CProps[A],
//                    st: => CState,
//                    pSt: Parent.PState[A]): SyntheticDragEvent[dom.Node] => Callback = { e =>
//      Callback {
//        if(!st) {
//          e.preventDefault()
//          e.dataTransfer.asInstanceOf[js.Dynamic].updateDynamic("dropEffect")("move")
//          p.onDragOver(a, pSt).runNow()
//        }
//      }
//    }
//
//    def drop[A](p: CProps[A]): SyntheticDragEvent[dom.Node] => Callback = { e =>
//      e.preventDefaultCB >> p.onMove
//    }
//
//    def renderDragHandle[S, A](p: CProps[A], a: A, st: CState) =
//      <.span(
//        ^.className := "draghandle",
//        ^.draggable := "true",
//        ^.onDragStart ==> dragStart(a, p),
//        ^.onDragEnd --> dragEnd(p),
//        "\u2630"
//      )
//
//    def renderRow[A](p: CProps[A], a: A, st: CState, pSt: Parent.PState[A]) =
//      <.div(
//        ^.className := { if(st) "dragging" else if(p.isDraggedOver) "dragover" },
//        ^.onDragEnter ==> (_.preventDefaultCB),
//        ^.onDragOver ==> dragOver(a, p, st, pSt),
//        ^.onDragLeave --> p.onDragLeave(pSt),
//        ^.onDrop -->
//
//
//      )
//      div(
//        classSet("dragging" -> T.state, "dragover" -> p.dragover)
//        ,onDragEnter ~~> preventDefaultIO
//        ,onDragOver  ~~> dragOver(a, p, T.state)
//        ,onDragLeave ~~> p.onDragLeave
//        ,onDrop      ~~> drop(p)
//      )
//
//    def dndItemComponent[A](r: (A, Tag) => Modifier ) = ReactComponentB[(A, DND.Child.CProps[A])]("DndItem")
//      .initialState(DND.Child.initialState)
//      .render(T => {
//        val (i,p) = T.props
//        DND.Child.renderRow(p, i, T)(
//          r(i, DND.Child.renderDragHandle(p, i, T))
//        )
//      }).create
//  }
//}
