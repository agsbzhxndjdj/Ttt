import '../models/site_movie.dart';
import 'internet_archive_service.dart';
import 'plex_service.dart';
import 'roku_service.dart';
import 'crackle_service.dart';

class SitesManager {
  static bool isSiteEnabled(String site) => true;

  static Future<List<SiteMovie>> getAllMovies({bool enrich = true}) async {
    final all = <SiteMovie>[];
    
    // 1. جلب من الأرشيف (زودنا الحد الأقصى لـ 50)
    all.addAll(await InternetArchiveService.getMovies(limit: 50));
    
    // 2. جلب من المصادر الأخرى مع عزل الأخطاء لطباعتها
    try {
      all.addAll(await PlexService.getMovies());
    } catch (e) { print('⚠️ Plex Error: $e'); }

    try {
      all.addAll(await RokuService.getMovies());
    } catch (e) { print('⚠️ Roku Error: $e'); }

    try {
      all.addAll(await CrackleService.getMovies());
    } catch (e) { print('⚠️ Crackle Error: $e'); }

    return _dedup(all);
  }

  static Future<List<SiteMovie>> getMoviesFromSite(String site, {bool enrich = true}) async {
    switch (site) {
      case 'plex': return await PlexService.getMovies();
      case 'roku': return await RokuService.getMovies();
      case 'crackle': return await CrackleService.getMovies();
      case 'archive':
      default: return await InternetArchiveService.getMovies(limit: 50);
    }
  }

  static Future<List<SiteMovie>> search(String query) async => [];

  static List<SiteMovie> _dedup(List<SiteMovie> list) {
    final seenIds = <String>{};
    // ✅ التصحيح: إزالة التكرار بناءً على المعرف الفريد (id) وليس العنوان
    return list.where((m) => seenIds.add(m.id)).toList();
  }
}
