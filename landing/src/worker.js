// Serves the ProHo landing page from this directory and sends every other
// host (e.g. a *.workers.dev preview) to proho.ilecto.app, keeping the path.
const CANONICAL_HOST = "proho.ilecto.app";

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.hostname !== CANONICAL_HOST) {
      url.hostname = CANONICAL_HOST;
      url.protocol = "https:";
      url.port = "";
      return Response.redirect(url.toString(), 301);
    }
    return env.ASSETS.fetch(request);
  },
};
