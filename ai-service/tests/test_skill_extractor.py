from app.nlp.skill_extractor import extract_skills


def names(text):
    return {m.name for m in extract_skills(text)}


def test_finds_basic_skills():
    assert names("Experienced in Java, Python and MySQL.") == {"Java", "Python", "MySQL"}


def test_java_is_not_found_inside_javascript():
    assert names("Frontend work in JavaScript") == {"JavaScript"}


def test_cpp_and_csharp():
    assert names("Wrote services in C++ and C#.") == {"C++", "C#"}


def test_dotted_names():
    assert names("Built APIs with Node.js and Express.js") == {"Node.js", "Express.js"}


def test_dotnet_and_aspnet():
    assert names("Developed with .NET and ASP.NET") == {".NET", "ASP.NET"}


def test_react_needs_exact_capitalisation():
    assert names("Built UIs with React and Redux") == {"React", "Redux"}
    assert names("We react quickly to change") == set()
    assert names("Used ReactJS daily") == {"React"}


def test_swift_needs_exact_capitalisation():
    assert names("Developed iOS apps in Swift.") == {"Swift"}
    assert names("Delivered features with swift turnaround") == set()


def test_does_not_invent_skills():
    assert names("Managed a team and improved processes") == set()
    assert names("I like coffee and long walks") == set()


def test_counts_and_matched_phrases():
    matches = {m.name: m for m in extract_skills("Spring Boot, springboot and Spring-Boot")}
    spring = matches["Spring Boot"]
    assert spring.count == 3
    assert set(spring.matched_as) == {"spring boot", "springboot"}


def test_version_numbers_do_not_block_matches():
    assert names("Java 17, Python3 and Spring Boot 3.2.") == {"Java", "Python", "Spring Boot"}


def test_ci_cd_variants():
    assert names("Set up CI/CD pipelines") == {"CI/CD"}
    assert names("Practised continuous delivery") == {"CI/CD"}


def test_empty_text():
    assert extract_skills("") == []


def test_node_js_does_not_claim_javascript():
    assert names("Used Node JS") == {"Node.js"}


def test_results_are_sorted_by_name():
    assert [m.name for m in extract_skills("Python Java python")] == ["Java", "Python"]