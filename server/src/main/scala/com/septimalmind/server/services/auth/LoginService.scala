package com.septimalmind.server.services.auth

import com.github.pshirshov.izumi.functional.bio.BIO
import com.septimalmind.server.idl.RequestContext
import com.septimalmind.services.auth._
import com.septimalmind.services.shared.{DomainFailure, _}
import scala.language.higherKinds

class LoginService[F[+ _, + _] : BIO] extends LoginServiceServer[F, RequestContext] {

  override def login
  (
    ctx: RequestContext,
    creds: Credentials
  ): F[DomainFailure, AccessResponse] = {
    creds match {
      case Credentials.EmailPassword(_) =>
        BIO[F].fail(AccessDenied(Some("not implemented")))
      case Credentials.PhonePassword(_) =>
        BIO[F].fail(AccessDenied(Some("not implemented")))
    }
  }

  override def signUp
  (
    ctx: RequestContext,
    request: SignUpRequest
  ): F[DomainFailure, AccessResponse] = {
    ???
  }


  override def logout
  (
    ctx: RequestContext
  ): F[DomainFailure, SuccessResponse] = {
    BIO[F].point(SuccessResponse(Some("logout")))
  }

}
