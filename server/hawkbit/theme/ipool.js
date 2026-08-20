// Vaadin selects its light/dark palette from a `theme` attribute on <html>,
// not from CSS media queries -- so honouring the OS preference needs one line
// of script. Kept deliberately tiny: no framework, no network, no tracking.
(function () {
  var mq = window.matchMedia('(prefers-color-scheme: dark)');
  function apply(dark) {
    document.documentElement.setAttribute('theme', dark ? 'dark' : '');
  }
  apply(mq.matches);
  mq.addEventListener('change', function (e) { apply(e.matches); });
})();
