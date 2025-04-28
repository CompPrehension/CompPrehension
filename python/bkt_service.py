import grpc, base64, pickle, sys, signal
from grpc import StatusCode
from concurrent import futures
from pyBKT.models import Roster
from pyBKT.models.Roster import SkillRoster
from proto import bkt_service_pb2, bkt_service_pb2_grpc

class BktServiceServicer(bkt_service_pb2_grpc.BktServiceServicer):

    def UpdateRoster(self, request, context):
        # ---------- 1. Десериализация ----------
        try:
            roster: Roster = pickle.loads(base64.b64decode(request.roster))
        except Exception as e:
            context.abort(StatusCode.INVALID_ARGUMENT, str(e))

        try:
            # ---------- 2. Обновление ----------
            skills_list = request.skills
            for skill in skills_list:
                # 2.1. если навык впервые встречается – заводим пустой SkillRoster,
                #      используя тот же pyBKT Model и настройки, что в исходном roster
                if skill not in roster.skill_rosters:
                    roster.skill_rosters[skill] = SkillRoster(
                        students=list(roster.students),
                        skill=skill,
                        mastery_state=roster.mastery_state,
                        track_progress=roster.track_progress,
                        model=roster.model,
                    )
                # 2.2. если студент новый – добавляем
                if request.student not in roster.skill_rosters[skill].students:
                    roster.skill_rosters[skill].add_student(request.student)
                # 2.3. апдейт состояния
                roster.update_state(skill, request.student, int(request.correct == True))
        except Exception as e:
            context.abort(StatusCode.INTERNAL, f"Python error: {e}")

        # ---------- 3. Сериализация результата ----------
        return bkt_service_pb2.UpdateRosterResponse(roster = base64.b64encode(pickle.dumps(roster)).decode("utf-8"))

def build_server(port: int = 50051) -> grpc.Server:
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=8))
    bkt_service_pb2_grpc.add_BktServiceServicer_to_server(BktServiceServicer(), server)
    server.add_insecure_port(f"[::]:{port}")
    return server

def graceful_shutdown(server: grpc.Server, *args):
    print("[PY] shutting down …", flush=True)
    server.stop(0)
    sys.exit(0)

if __name__ == "__main__":
    port = 50051
    grpc_server = build_server(port = port)
    signal.signal(signal.SIGTERM, lambda *a: graceful_shutdown(grpc_server))
    signal.signal(signal.SIGINT,  lambda *a: graceful_shutdown(grpc_server))
    
    grpc_server.start()
    print(f"[PY] BKT-service listening on :{port}", flush=True)
    grpc_server.wait_for_termination()
