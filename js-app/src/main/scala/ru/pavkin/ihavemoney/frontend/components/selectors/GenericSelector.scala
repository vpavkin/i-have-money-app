package ru.pavkin.ihavemoney.frontend.components.selectors

import cats.syntax.applicative._
import cats.syntax.foldable._
import cats.instances.option._
import cats.{Applicative, Foldable, Id}
import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{ReactComponentB, ReactComponentU, _}
import ru.pavkin.ihavemoney.domain.Identified.ops._
import ru.pavkin.ihavemoney.domain.Named.ops._
import ru.pavkin.ihavemoney.domain.{Identified, Named}
import ru.pavkin.ihavemoney.frontend.bootstrap.attributes._
import ru.pavkin.ihavemoney.frontend.components.ReactHelpers.{dontSubmit, onInputChange}
import ru.pavkin.ihavemoney.frontend.components.selectors.GenericSelector.SearchInterface
import ru.pavkin.ihavemoney.frontend.styles.Global._
import ru.pavkin.utils.strings.syntax._

import scalacss.ScalaCssReact._

abstract class GenericSelector[F[_] : Applicative : Foldable, T: Named : Identified] {

  trait PropsTemplate {
    def selected: F[T]
    def onChange: F[T] => Callback
    def all: List[T]
    def style: common.context
    def maxElements: Int
    def htmlIdPrefix: String
    def addAttributes: Seq[TagMod]
  }

  type TheProps <: PropsTemplate

  // default elements to always be rendered, like "Not Selected" for F = Option
  def defaultElements: List[(String, F[T])] = Nil

  def sortItems(pr: TheProps)(all: List[T]): List[T] = all
  def searchInterface: SearchInterface = SearchInterface.NoSearch

  def elementTemplate(pr: TheProps)(e: T, addAttributes: Seq[TagMod] = Seq()): TagMod = li(
    id := s"${pr.htmlIdPrefix}-${e.uid}",
    cls := s"${pr.htmlIdPrefix}-element",
    key := e.uid,
    a(href := "#", onClick ==> ((evt: ReactEventI) => dontSubmit(evt) >> pr.onChange(e.pure[F])), e.name),
    addAttributes
  )

  def renderElement(pr: TheProps)(e: T): TagMod =
    elementTemplate(pr)(e, Seq.empty)

  def filterByMatchingSearchText(searchText: String)(items: List[T]): List[T] =
    if (searchText.isEmpty) items
    else items.filter(e => e.name.toLowerCase.contains(searchText))

  def searchMatchRank(searchText: String)(name: String): Int =
    if (name.split("\\s+").exists(_.startsWith(searchText))) 0
    else if (name.contains(searchText)) 10
    else 20

  def sortBySearchMatchRank(pr: TheProps, searchText: String)(items: List[T]): List[T] =
    if (searchText.isEmpty) items
    else {
      val ranker = searchMatchRank(searchText) _
      items.sortBy(e => ranker(e.name.toLowerCase))
    }

  def defaultTitle: String = "Not selected"

  case class State(searchText: String = "") {
    val lowercaseSearchText: String = searchText.toLowerCase
  }

  class Backend(scope: BackendScope[TheProps, State]) {
    def render(props: TheProps, state: State) = div(common.dropdown,
      button(
        common.buttonOpt(props.style),
        tpe := "button",
        props.addAttributes,
        className := "dropdown-toggle",
        dataToggle := "dropdown",
        aria.haspopup := true,
        aria.expanded := false,
        props.selected.foldLeft(defaultTitle) { case (_, e) => e.name },
        span(common.caret)
      ),
      ul(common.dropdownMenu,
        role := "menu",
        cls := "generic-selector",
        renderSearchField(props, state),
        renderDefaultElements(props),
        renderElements(props, state)
      )
    )

    private def renderSearchField(pr: TheProps, st: State) =
      searchInterface match {
        case SearchInterface.NoSearch => EmptyTag
        case _ => li(input.text(
          id := s"${pr.htmlIdPrefix}-search-field",
          value := st.searchText, placeholder := "Search...", common.formControl,
          onChange ==> onInputChange(text => scope.setState(State(text)))
        ))
      }

    private def renderDefaultElements(pr: TheProps) =
      defaultElements.map {
        case (theName, element) => li(
          id := s"${pr.htmlIdPrefix}-${theName.cssClass}",
          key := theName.cssClass,
          a(href := "#", onClick ==> ((evt: ReactEventI) => dontSubmit(evt) >> pr.onChange(element)), theName)
        )
      }

    private def renderElements(pr: TheProps, st: State) =
      sortBySearchRank(pr, st)(
        filterByMatchingSearchText(st.lowercaseSearchText)(
          sortItems(pr)(pr.all)))
        .take(pr.maxElements)
        .map(renderElement(pr))

    private def sortBySearchRank(pr: TheProps, st: State)(items: List[T]) =
      searchInterface match {
        case SearchInterface.NoSearch => items
        case SearchInterface.TextSearch => sortBySearchMatchRank(pr, st.lowercaseSearchText)(items)
      }
  }

  val component = ReactComponentB[TheProps]("GenericSelector")
    .initialState(State())
    .renderBackend[Backend]
    .build
}

abstract class SimpleGenericSelector[F[_] : Applicative : Foldable, T: Named : Identified]
  extends GenericSelector[F, T] {

  case class Props(
    selected: F[T],
    onChange: F[T] => Callback,
    all: List[T],
    style: common.context = common.context.success,
    maxElements: Int = 30,
    htmlIdPrefix: String = "",
    addAttributes: Seq[TagMod] = Seq()) extends PropsTemplate

  type TheProps = Props

  /**
    * @param selected     current selected element
    * @param onChange     function to call on selected element change
    * @param all          all elements to select from
    * @param htmlIdPrefix prefix string to add to each dropdown element own id attribute.
    */
  def apply(
    selected: F[T],
    onChange: F[T] => Callback,
    all: List[T],
    style: common.context = common.context.success,
    maxElements: Int = 30,
    htmlIdPrefix: String = "",
    addAttributes: Seq[TagMod] = Seq()): ReactComponentU[TheProps, State, Backend, TopNode] =
    component(Props(selected, onChange, all, style, maxElements, htmlIdPrefix, addAttributes))
}

abstract class StrictSimpleSelector[T: Named : Identified] extends SimpleGenericSelector[Id, T]
abstract class OptionalSimpleSelector[T: Named : Identified] extends SimpleGenericSelector[Option, T] {
  override def defaultElements: List[(String, Option[T])] = List("Not Selected" -> None)
}

object GenericSelector {

  sealed trait SearchInterface
  object SearchInterface {
    case object NoSearch extends SearchInterface
    case object TextSearch extends SearchInterface
  }
}
