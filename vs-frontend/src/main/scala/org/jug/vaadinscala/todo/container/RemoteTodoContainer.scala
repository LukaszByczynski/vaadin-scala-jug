package org.jug.vaadinscala.todo.container

import java.util
import java.util.{Collection => JCollection, HashMap => JHashMap, List => JList, UUID}

import com.vaadin.data.Container.ItemSetChangeListener
import com.vaadin.data.util.{AbstractContainer, BeanItem}
import com.vaadin.data.{Container, Item, Property}
import com.vaadin.ui.Notification
import org.jug.vaadinscala.todo.akka.TypedActorProxy
import org.jug.vaadinscala.todo.async.Async
import org.jug.vaadinscala.todo.{Todo, TodoRepository}

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.reflect.runtime.universe._

abstract class RemoteTodoContainer(source: TypedActorProxy[TodoRepository])
  extends AbstractContainer with Container.ItemSetChangeNotifier with Container.Indexed with Container.Sortable {

  private val _propertiesMap = new JHashMap[String, Class[_]]

  private val _nestedProperties = ArrayBuffer[String]()

  var _fetching = false

  private var _ids: Option[IndexedSeq[Integer]] = None

  private var _items: Option[IndexedSeq[BeanItem[Todo]]] = None

  def dispose(): Unit = {
    source.dispose()
  }

  init()

  /** Gets BeanItem with given id from container
    *
    * @param itemId the item id
    * @return  an option bean containing BeanItem
    */
  def getBeanItem(itemId: Any): Option[BeanItem[Todo]] = {
    indexOfId(itemId) match {
      case -1 => None
      case pos => Some(_items.get.get(pos))
    }
  }

  /**
   * Gets the Item with the given Item ID from the Container. If the
   * Container does not contain the requested Item, <code>null</code> is
   * returned.
   * <p>
   * Containers should not return Items that are filtered out.
   *
   * @param itemId
     * ID of the Item to retrieve
   * @return the Item with the given ID or <code>null</code> if the
   *         Item is not found in the Container
   */
  override def getItem(itemId: Any): Item = {
    getBeanItem(itemId) match {
      case Some(item) => item
      case _ => null
    }
  }

  /**
   * Gets the ID's of all Properties stored in the Container. The ID's cannot
   * be modified through the returned collection.
   *
   * @return unmodifiable collection of Property IDs
   */
  override def getContainerPropertyIds: JCollection[_] = {
    _propertiesMap.keySet()
  }

  /**
   * Gets the Property identified by the given itemId and propertyId from the
   * Container. If the Container does not contain the item or it is filtered
   * out, or the Container does not have the Property, <code>null</code> is
   * returned.
   *
   * @param itemId
   * ID of the visible Item which contains the Property
   * @param propertyId
   * ID of the Property to retrieve
   * @return Property with the given ID or <code>null</code>
   */
  override def getContainerProperty(itemId: AnyRef, propertyId: AnyRef): Property[_] = {
    getItem(itemId).getItemProperty(propertyId)
  }

  /**
   * Gets the ID's of all visible (after filtering and sorting) Items stored
   * in the Container. The ID's cannot be modified through the returned
   * collection.
   * <p>
   * If the container is Ordered, the collection returned by this
   * method should follow that order. If the container is Sortable,
   * the items should be in the sorted order.
   * <p>
   * Calling this method for large lazy containers can be an expensive
   * operation and should be avoided when practical.
   *
   * @return unmodifiable collection of Item IDs
   */
  override def getItemIds: JCollection[_] = {
    ids.toIterable
  }

  /**
   * Gets the data type of all Properties identified by the given Property ID.
   *
   * @param propertyId
   * ID identifying the Properties
   * @return data type of the Properties
   */
  override def getType(propertyId: AnyRef): Class[_] = {
    _propertiesMap.get(propertyId)
  }

  /**
   * Gets the number of visible Items in the Container.
   * <p>
   * Filtering can hide items so that they will not be visible through the
   * container API.
   *
   * @return number of Items in the Container
   */
  override def size(): Int = {
    ids.size
  }

  /**
   * Tests if the Container contains the specified Item.
   * <p>
   * Filtering can hide items so that they will not be visible through the
   * container API, and this method should respect visibility of items (i.e.
   * only indicate visible items as being in the container) if feasible for
   * the container.
   *
   * @param itemId
   * ID the of Item to be tested
   * @return boolean indicating if the Container holds the specified Item
   */
  override def containsId(itemId: Any): Boolean = {
    ids.contains(itemId)
  }

  override def addItem(itemId: AnyRef): Item = {
    throw new UnsupportedOperationException("Read only container")
  }

  override def addItem(): AnyRef = {
    throw new UnsupportedOperationException("Read only container")
  }

  override def removeItem(itemId: AnyRef): Boolean = {
    throw new UnsupportedOperationException("Read only container")
  }

  override def addContainerProperty(propertyId: AnyRef, `type`: Class[_], defaultValue: AnyRef): Boolean = {
    throw new UnsupportedOperationException("Read only container")
  }

  override def removeContainerProperty(propertyId: AnyRef): Boolean = {
    throw new UnsupportedOperationException("Read only container")
  }

  override def removeAllItems(): Boolean = {
    throw new UnsupportedOperationException("Read only container")
  }

  def refresh(onFinish: => Unit = {}): Unit = {
    if (!_fetching) {
      _fetching = true
      fetchIds(
        result => {
          val fetchedIds = result.map(Int.box).toIndexedSeq
          _ids = Some(fetchedIds)
          if (fetchedIds.nonEmpty) {
            fetchByIds(result)(
              items => {
                _items = Some(items.map(_makeBeanItem).toIndexedSeq)
                fireItemSetChange()
                onFinish
                _fetching = false
              }
            )
          } else {
            fireItemSetChange()
            onFinish
            _fetching = false
          }
        }
      )
    }
  }

  def refreshItem(itemId: Int): Unit = {
    if (_items.getOrElse(Seq()).exists(_.getBean.id == itemId)) {
      _safeFetch(source().findByIds(Seq(itemId)))(
        success => {
          _items = Some(
            _items.get.map(i => if (i.getBean.id == itemId) _makeBeanItem(success.head) else i).toIndexedSeq
          )
          fireItemSetChange()
        }
      )
    }
  }

  private def _safeFetch[A](future: Future[A])(success: (A => Unit), failure: Throwable => Unit = (_) => {}) = {
    Async.default(future)(
      data => success(data),
      error => {
        _showBackendFailure(error)
        failure(error)
      }
    )
  }

  private def _showBackendFailure(t: Throwable): Unit = {
    val uuid = UUID.randomUUID()
    Notification.show(
      "Backend services are unavailable right now.\nPlease try again later.",
      s"id: $uuid",
      Notification.Type.ERROR_MESSAGE
    )
  }

  private def _makeBeanItem(item: Todo): BeanItem[Todo] = {
    val beanItem = new BeanItem[Todo](item)
    _nestedProperties.foreach(beanItem.addNestedProperty)
    beanItem
  }

  override def removeItemSetChangeListener(listener: ItemSetChangeListener): Unit = {
    super.removeItemSetChangeListener(listener)
  }

  override def addItemSetChangeListener(listener: ItemSetChangeListener): Unit = {
    super.addItemSetChangeListener(listener)
  }

  override def addItemAfter(previousItemId: AnyRef): AnyRef = {
    throw new UnsupportedOperationException("Read only container")
  }

  override def addItemAfter(previousItemId: AnyRef, newItemId: AnyRef): Item = {
    throw new UnsupportedOperationException("Read only container")
  }

  override def addItemAt(index: Int): AnyRef = {
    throw new UnsupportedOperationException("Read only container")
  }

  override def addItemAt(index: Int, newItemId: AnyRef): Item = {
    throw new UnsupportedOperationException("Read only container")
  }

  /**
   * Gets the index of the Item corresponding to the itemId. The following
   * is <code>true</code> for the returned index: 0 <= index < size(), or
   * index = -1 if there is no visible item with that id in the container.
   *
   * @param itemId
   * ID of an Item in the Container
   * @return index of the Item, or -1 if (the filtered and sorted view of)
   *         the Container does not include the Item
   */
  override def indexOfId(itemId: Any): Int = {
    ids.indexOf(itemId)
  }

  /**
   * Get <code>numberOfItems</code> consecutive item ids from the
   * container, starting with the item id at <code>startIndex</code>.
   * <p>
   * Implementations should return at most <code>numberOfItems</code> item
   * ids, but can contain less if the container has less items than
   * required to fulfill the request. The returned list must hence contain
   * all of the item ids from the range:
   * <p>
   * <code>startIndex</code> to
   * <code>max(startIndex + (numberOfItems-1), container.size()-1)</code>.
   * <p>
   *
   * @param startIndex
   * the index for the first item which id to include
   * @param numberOfItems
   * the number of consecutive item ids to get from the given
   * start index, must be >= 0
   * @return List containing the requested item ids or empty list if
   *         <code>numberOfItems</code> == 0; not null
   *
   * @throws IllegalArgumentException
   * if <code>numberOfItems</code> is < 0
   * @throws IndexOutOfBoundsException
   * if <code>startIndex</code> is outside the range of the
   * container. (i.e.
   * <code>startIndex &lt; 0 || container.size()-1 &lt; startIndex</code>
   * )
   *
   * @since 7.0
   */
  override def getItemIds(startIndex: Int, numberOfItems: Int): JList[_] = {
    if (startIndex < 0 || ids.size < startIndex)
      throw new IndexOutOfBoundsException()
    ids.drop(startIndex).take(numberOfItems).toList
  }

  /**
   * Get the item id for the item at the position given by
   * <code>index</code>.
   * <p>
   *
   * @param index
   * the index of the requested item id
   * @return the item id of the item at the given index
   * @throws IndexOutOfBoundsException
   * if <code>index</code> is outside the range of the
   * container. (i.e.
   * <code>index &lt; 0 || container.size()-1 &lt; index</code>
   * )
   */
  override def getIdByIndex(index: Int): AnyRef = {
    if (index < 0 || (ids.size - 1) < index)
      throw new IndexOutOfBoundsException()
    ids.get(index)
  }

  /**
   * Gets the ID of the Item preceding the Item that corresponds to
   * <code>itemId</code>. If the given Item is the first or not found in
   * the Container, <code>null</code> is returned.
   *
   * @param itemId
   * ID of a visible Item in the Container
   * @return ID of the previous visible Item or <code>null</code>
   */
  override def prevItemId(itemId: AnyRef): AnyRef = {
    indexOfId(itemId) match {
      case -1 | 0 => null
      case pos => ids.get(pos - 1)
    }
  }

  /**
   * Gets the ID of the last Item in the Container..
   *
   * @return ID of the last visible Item in the Container
   */
  override def lastItemId(): AnyRef = {
    ids.last
  }

  /**
   * Gets the ID of the Item following the Item that corresponds to
   * <code>itemId</code>. If the given Item is the last or not found in
   * the Container, <code>null</code> is returned.
   *
   * @param itemId
   * ID of a visible Item in the Container
   * @return ID of the next visible Item or <code>null</code>
   */
  override def nextItemId(itemId: AnyRef): AnyRef = {
    val lastItemIndex = ids.size - 1
    indexOfId(itemId) match {
      case -1 | `lastItemIndex` => null
      case pos => ids.get(pos + 1)
    }
  }

  /**
   * Gets the ID of the first Item in the Container.
   *
   * @return ID of the first visible Item in the Container
   */
  override def isFirstId(itemId: AnyRef): Boolean = {
    ids.head.asInstanceOf[AnyRef] == itemId
  }

  /**
   * Gets the ID of the last Item in the Container..
   *
   * @return ID of the last visible Item in the Container
   */
  override def isLastId(itemId: AnyRef): Boolean = {
    ids.last.asInstanceOf[AnyRef] == itemId
  }

  /**
   * Gets the ID of the first Item in the Container.
   *
   * @return ID of the first visible Item in the Container
   */
  override def firstItemId(): AnyRef = {
    ids.head
  }

  //  protected def getItems: Seq[BeanItem[Todo]] = {
  //    _items.getOrElse {
  //      _items = {
  //        val ids = fetchIds()
  //        if (ids.size > 0)
  //          Some(fetchByIds(ids).map {
  //            new BeanItem[Todo](_)
  //          }.toSeq)
  //        else
  //          Some(Seq())
  //      }
  //      _items.get
  //    }
  //  }
  override def sort(propertyId: Array[AnyRef], ascending: Array[Boolean]): Unit = {}

  override def getSortableContainerPropertyIds: JCollection[_] = new util.ArrayList[Todo]()

  /** Gets ids of the items managed by container */
  protected def findIds(): Future[Seq[Int]]

  /** Initialize container */
  protected def init() {
    // get properties s
    TypeTagCache.get(typeTag[Todo]) match {
      case (propertyMap, nested) =>
        _propertiesMap.putAll(propertyMap)
        _nestedProperties ++= nested
    }
  }

  /** Gets IndexedSeq of the ids */
  protected def ids: IndexedSeq[Integer] = {
    _ids match {
      case Some(data) => data
      case None =>
        refresh()
        IndexedSeq()
    }
  }

  /** Fetch ids from remote source */
  protected def fetchIds(result: Seq[Int] => Any): Unit = {
    _safeFetch(findIds())(
      success => result(success),
      failure => result(Seq())
    )
  }

  /** Fetch items from remote source */
  protected def fetchByIds(ids: Seq[Int])(result: Seq[Todo] => Any) = {
    _safeFetch(source().findByIds(ids))(
      success => result(success),
      failure => result(Seq())
    )
  }

  private object TypeTagCache {
    private val _cache: TrieMap[TypeTag[_], (JHashMap[String, Class[_]], Seq[String])] = new TrieMap()

    private val _mirror = runtimeMirror(this.getClass.getClassLoader)

    /** Gets list of the properties and nested properties names for given type tag
      */
    def get(tag: TypeTag[_]): (JHashMap[String, Class[_]], Seq[String]) = {
      _cache.getOrElseUpdate(
      tag, {
        val propertiesMap = new JHashMap[String, Class[_]]
        val nestedProperties = ArrayBuffer[String]()

        // recursive scan for tag symbols
        def scanSymbols(prefix: Option[String], symbols: Seq[Symbol]): Unit = {
          symbols
            .collect { case s: TermSymbol => s }
            .filter(s => s.annotations.exists(a => a.tree.tpe == typeOf[BeanProperty]) && (s.isVal || s.isVar))
            .foreach(
              field => {
                var name = field.name.toString.trim
                if (prefix.isDefined) {
                  // nested property
                  name = s"${prefix.get}.$name"
                  nestedProperties += name
                }
                if (field.typeSignature.typeSymbol.isClass)
                  scanSymbols(Some(name), field.typeSignature.members.toSeq)
                propertiesMap.put(name, _mirror.runtimeClass(field.typeSignature.finalResultType))
              }
            )
        }
        scanSymbols(None, tag.tpe.members.toSeq)
        (propertiesMap, nestedProperties)
      }
      )
    }
  }

}
