package di

import scala.reflect.runtime.universe.{TypeTag, typeTag}
import scala.collection.mutable

trait ContextObjectRegistry {
  import ContextObjectRegistry._

  private val parentContexts = mutable.MutableList[ContextObjectRegistry]()
  private val internal = mutable.MutableList[(TypeTag[_], Any)]()

  def shareContextToChild[ChildType <: ContextObjectRegistry : TypeTag](child: ChildType): Unit = {
    val tags = getContextTags
    child.internal.find(t => tags.exists(_.tpe =:= t._1.tpe)) match {
      case None => child.parentContexts += this
      case Some(_) => throw new AlreadyInParentContextException(typeTag[ChildType])
    }
  }

  final def addComponentToContext[ChildType: TypeTag](value: ChildType): Unit = {
    val typeTg = typeTag[ChildType]
    lookupInContext[ChildType] match {
      case None => internal += typeTg -> value
      case Some(_) => throw new AlreadyInContextException(typeTg)
    }
  }

  def getSingletonObject[T: TypeTag]: T = {
    lookupInContext[T] match {
      case Some(value) => value
      case None => throw new NotInContextException(typeTag[T])
    }
  }

  private def lookupInContext[T: TypeTag]: Option[T] = {
    val typeTg = typeTag[T]
    internal.find(_._1.tpe =:= typeTg.tpe).map { value =>
      value._2.asInstanceOf[T]
    }.orElse {
      parentContexts
        .find(_.lookupInContext[T].isDefined)
        .flatMap(_.lookupInContext[T])
    }
  }

  private def getContextTags: List[TypeTag[_]] = {
    parentContexts.flatMap(_.getContextTags).toList ++ internal.map(_._1).toList
  }
}

object ContextObjectRegistry {
  class AlreadyInContextException(t: TypeTag[_]) extends RuntimeException(s"Element of type ${t.tpe.typeSymbol} is already present in context registry")
  class AlreadyInParentContextException(t: TypeTag[_]) extends RuntimeException(s"Element of type ${t.tpe.typeSymbol} is already present in parent context registry")
  class NotInContextException(t: TypeTag[_]) extends RuntimeException(s"No Element of type ${t.tpe.typeSymbol} in context")
}
