package ru.pavkin.ihavemoney.frontend.redux

import diode.data.{Failed, Pot, Ready}
import diode.{ActionType, Circuit, ModelR, ModelRO}
import japgolly.scalajs.react.{Callback, ReactElement}
import ru.pavkin.ihavemoney.frontend.redux.actions.{HideModal, ShowModal}

trait CircuitHelpers[M <: AnyRef] { self: Circuit[M] =>

  type Unsubscriber = () => Unit

  /**
    * Subscribes to changes in the cursor and passes an unsubscribe hook to the listener.
    * Gives the listener control to unsubscribe from updates.
    */
  def subscribeU[T](
    cursor: ModelR[M, T])
    (listener: (Unsubscriber, ModelRO[T]) => Unit): Unit = {
    var unsubscribe: () => Unit = () => ()
    unsubscribe = subscribe(cursor)(m => listener(unsubscribe, m))
  }

  /**
    * Subscribes to pot updates and allows to react on failure and success cases.
    * After either failure or success happens, listener is automatically unsubscribed
    */
  def subscribePotOnce[T](
    cursor: ModelR[M, Pot[T]])(
    onFailure: Throwable => Unit = _ => (),
    onSuccess: T => Unit = (t: T) => ()): Unit =
    subscribeU(cursor) { (unsubscribe, pot) =>
      pot() match {
        case Ready(x) =>
          unsubscribe()
          onSuccess(x)
        case Failed(e) =>
          unsubscribe()
          onFailure(e)
        case _ => ()
      }
    }

  def showModal(modal: ReactElement): Unit = dispatch(ShowModal(modal))
  def hideModal(): Unit = dispatch(HideModal)
  def hideModalCB() = Callback(hideModal())

  def dispatchCB[A: ActionType](action: A): Callback = Callback(dispatch(action))

  def stateCursor: ModelR[M, M] = zoom(identity)
  def state: M = stateCursor.value

}
