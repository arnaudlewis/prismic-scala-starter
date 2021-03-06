package controllers

import play.api.mvc._

import scala.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

import io.prismic._
import io.prismic.fragments._

object Application extends Controller with PrismicController {

  import PrismicHelper._

  // -- Resolve links to documents
  def linkResolver(api: Api)(implicit request: RequestHeader) = DocumentLinkResolver(api) {
    case (docLink, maybeBookmarked) if !docLink.isBroken => routes.Application.slicepage(docLink.uid.getOrElse(throw new IllegalArgumentException("The UID must be initialised. It can't be null."))).absoluteURL()
    case _ => routes.Application.brokenLink().absoluteURL()
  }

  // -- Page not found
  def PageNotFound(implicit ctx: PrismicHelper.Context) = NotFound(views.html.pageNotFound())

  def brokenLink = PrismicAction { implicit request =>
    Future.successful(PageNotFound)
  }

  def index = PrismicAction { implicit request =>
    for(
      maybeDocument <- getBookmarkedDocument("home");
      maybeSkin <- getBookmarkedDocument("skin")
    ) yield (
      maybeDocument match {
        case Some(document) => Ok(views.html.index(document, maybeSkin))
        case None => Redirect(routes.Application.brokenLink().absoluteURL())
      }
    )
  }

   // -- Slicepage
   def slicepage(uid: String) = PrismicAction { implicit request =>

    getDocumentByUid("slicepage", uid).flatMap { maybePage =>
      maybePage.map { page =>
        val currentUid = page.uid.getOrElse(throw new IllegalArgumentException("The UID must be initialised. It can't be null."));
        if(currentUid != uid)
          Future.successful(Results.Redirect(routes.Application.slicepage(uid)))
        else 
          getBookmarkedDocument("skin").map { maybeSkin => 
            Ok(views.html.slicepage(page, maybeSkin))
          }
        } getOrElse(Future.successful(Results.Redirect(routes.Application.brokenLink().absoluteURL())))
      } 
    }
  }