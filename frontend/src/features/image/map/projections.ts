import { register } from "ol/proj/proj4";
import proj4 from "proj4";

const UTM_ZONE_COUNT = 60;

export function registerUtmProjections() {
    for (let zone = 1; zone <= UTM_ZONE_COUNT; zone++) {
        const code = String(zone).padStart(2, "0");
        proj4.defs(
            `EPSG:326${code}`,
            `+proj=utm +zone=${zone} +datum=WGS84 +units=m +no_defs`,
        );
        proj4.defs(
            `EPSG:327${code}`,
            `+proj=utm +zone=${zone} +south +datum=WGS84 +units=m +no_defs`,
        );
    }
    register(proj4);
}
