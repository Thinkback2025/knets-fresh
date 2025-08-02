import { Button } from "@/components/ui/button";
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from "@/components/ui/alert-dialog";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiRequest } from "@/lib/queryClient";
import { useToast } from "@/hooks/use-toast";
import { isUnauthorizedError } from "@/lib/authUtils";
import { Smartphone, Tablet, Settings, Lock, Unlock, Calendar, Trash2, CheckCircle } from "lucide-react";

interface DeviceCardProps {
  device: {
    id: number;
    name: string;
    imei: string;
    phoneNumber: string;
    deviceType: string;
    model?: string;
    isActive: boolean;
    isLocked: boolean;
    lastSeen?: string;
    screenTimeToday?: number;
    consentStatus: string;
  };
}

export function DeviceCard({ device }: DeviceCardProps) {
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const toggleLockMutation = useMutation({
    mutationFn: async (isLocked: boolean) => {
      await apiRequest("PATCH", `/api/devices/${device.id}/lock`, { isLocked });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["/api/devices"] });
      queryClient.invalidateQueries({ queryKey: ["/api/dashboard/stats"] });
      toast({
        title: device.isLocked ? "Device Unlocked" : "Device Locked",
        description: `${device.name} has been ${device.isLocked ? 'unlocked' : 'locked'} successfully`,
      });
    },
    onError: (error) => {
      if (isUnauthorizedError(error)) {
        toast({
          title: "Unauthorized",
          description: "You are logged out. Logging in again...",
          variant: "destructive",
        });
        setTimeout(() => {
          window.location.href = "/api/login";
        }, 500);
        return;
      }
      toast({
        title: "Error",
        description: "Failed to update device status",
        variant: "destructive",
      });
    },
  });

  const removeDeviceMutation = useMutation({
    mutationFn: async () => {
      await apiRequest("DELETE", `/api/devices/${device.id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["/api/devices"] });
      queryClient.invalidateQueries({ queryKey: ["/api/dashboard/stats"] });
      queryClient.invalidateQueries({ queryKey: ["/api/children"] });
      toast({
        title: "Device Removed",
        description: `${device.name} has been removed successfully`,
      });
    },
    onError: (error) => {
      if (isUnauthorizedError(error)) {
        toast({
          title: "Unauthorized",
          description: "You are logged out. Logging in again...",
          variant: "destructive",
        });
        setTimeout(() => {
          window.location.href = "/api/login";
        }, 500);
        return;
      }
      toast({
        title: "Error",
        description: "Failed to remove device",
        variant: "destructive",
      });
    },
  });

  const approveDeviceMutation = useMutation({
    mutationFn: async () => {
      await apiRequest("PATCH", `/api/devices/${device.id}/approve`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["/api/devices"] });
      queryClient.invalidateQueries({ queryKey: ["/api/dashboard/stats"] });
      toast({
        title: "Device Approved",
        description: `${device.name} has been approved for control`,
      });
    },
    onError: (error) => {
      if (isUnauthorizedError(error)) {
        toast({
          title: "Unauthorized",
          description: "You are logged out. Logging in again...",
          variant: "destructive",
        });
        setTimeout(() => {
          window.location.href = "/api/login";
        }, 500);
        return;
      }
      toast({
        title: "Error",
        description: "Failed to approve device",
        variant: "destructive",
      });
    },
  });

  const getStatusColor = () => {
    if (device.consentStatus === 'pending') return 'text-warning-orange';
    if (device.consentStatus === 'denied') return 'text-error-red';
    if (device.isLocked) return 'text-error-red';
    if (device.isActive && device.lastSeen && new Date(device.lastSeen).getTime() > Date.now() - 300000) {
      return 'text-success-green';
    }
    return 'text-neutral-dark';
  };

  const getStatusText = () => {
    if (device.consentStatus === 'pending') return 'Pending Consent';
    if (device.consentStatus === 'denied') return 'Consent Denied';
    if (device.isLocked) return 'Locked';
    if (device.isActive && device.lastSeen && new Date(device.lastSeen).getTime() > Date.now() - 300000) {
      return 'Online';
    }
    return 'Offline';
  };

  const formatScreenTime = (minutes?: number) => {
    if (!minutes) return '0h 0m';
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return `${hours}h ${mins}m`;
  };

  const maskedImei = device.imei.replace(/(\d{3})\d{9}(\d{3})/, '$1*****$2');
  const maskedPhone = (() => {
    // Handle different phone number formats with country codes
    const phone = device.phoneNumber;
    if (phone.includes('+')) {
      const parts = phone.split(' ');
      if (parts.length >= 2) {
        const countryCode = parts[0];
        const number = parts.slice(1).join('');
        if (number.length >= 7) {
          const visibleStart = number.substring(0, 3);
          const visibleEnd = number.substring(number.length - 2);
          return `${countryCode} ${visibleStart}***${visibleEnd}`;
        }
      }
    }
    // Fallback for other formats
    return phone.replace(/(\+?\d{1,3})\s*(.{3}).+(.{2})/, '$1 $2***$3');
  })();

  return (
    <div className="flex items-center justify-between p-4 border border-neutral-medium rounded-lg hover:shadow-md transition-shadow">
      <div className="flex items-center space-x-4 flex-1 min-w-0">
        <div className="w-12 h-12 bg-neutral-medium rounded-lg flex items-center justify-center flex-shrink-0">
          {device.deviceType === 'tablet' ? (
            <Tablet className="w-6 h-6 text-neutral-dark" />
          ) : (
            <Smartphone className="w-6 h-6 text-neutral-dark" />
          )}
        </div>
        <div className="flex-1 min-w-0">
          <h4 className="font-medium text-neutral-darker truncate">{device.name}</h4>
          <p className="text-sm text-neutral-dark truncate">{device.model}</p>
          <p className="text-xs text-neutral-dark truncate">IMEI: {maskedImei}</p>
          <p className="text-xs text-neutral-dark truncate">Phone: {maskedPhone}</p>
        </div>
      </div>
      
      <div className="flex items-center space-x-3 flex-shrink-0">
        <div className="text-right">
          <div className="flex items-center space-x-2 mb-1 justify-end">
            <div className={`w-2 h-2 rounded-full ${
              device.consentStatus === 'pending' ? 'bg-warning-orange' :
              device.consentStatus === 'denied' ? 'bg-error-red' :
              device.isLocked ? 'bg-error-red' :
              device.isActive && device.lastSeen && new Date(device.lastSeen).getTime() > Date.now() - 300000 ? 'bg-success-green' :
              'bg-neutral-dark'
            }`}></div>
            <span className={`text-sm font-medium ${getStatusColor()}`}>
              {getStatusText()}
            </span>
          </div>
          <p className="text-xs text-neutral-dark text-right">
            {device.isLocked ? 'Locked by schedule' : `Screen time: ${formatScreenTime(device.screenTimeToday)}`}
          </p>
        </div>
        
        <div className="flex items-center space-x-1">
          <Button 
            variant="ghost" 
            size="sm"
            className="p-1.5 text-neutral-dark hover:text-green-600"
            onClick={() => {
              toast({
                title: "Schedule Manager",
                description: "Schedule management feature coming soon",
              });
            }}
          >
            <Calendar className="w-3.5 h-3.5" />
          </Button>
          {device.consentStatus === 'pending' && (
            <Button 
              variant="ghost" 
              size="sm"
              className="p-1.5 text-neutral-dark hover:text-green-600"
              onClick={() => approveDeviceMutation.mutate()}
              disabled={approveDeviceMutation.isPending}
              title="Approve Device"
            >
              <CheckCircle className="w-3.5 h-3.5" />
            </Button>
          )}
          {device.consentStatus === 'approved' && (
            <Button 
              variant="ghost" 
              size="sm"
              className={`p-1.5 ${device.isLocked ? 'text-green-600 hover:text-success-green' : 'text-neutral-dark hover:text-error-red'}`}
              onClick={() => toggleLockMutation.mutate(!device.isLocked)}
              disabled={toggleLockMutation.isPending}
              title={device.isLocked ? "Unlock Device" : "Lock Device"}
            >
              {device.isLocked ? <Unlock className="w-3.5 h-3.5" /> : <Lock className="w-3.5 h-3.5" />}
            </Button>
          )}
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button 
                variant="ghost" 
                size="sm"
                className="p-1.5 text-neutral-dark hover:text-error-red"
                disabled={removeDeviceMutation.isPending}
                title="Delete Device"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Remove Device</AlertDialogTitle>
                <AlertDialogDescription>
                  Are you sure you want to remove "{device.name}"? This action cannot be undone. All device data, schedules, and activity logs will be permanently deleted.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Cancel</AlertDialogCancel>
                <AlertDialogAction 
                  onClick={() => removeDeviceMutation.mutate()}
                  className="bg-error-red hover:bg-red-700"
                  disabled={removeDeviceMutation.isPending}
                >
                  {removeDeviceMutation.isPending ? "Removing..." : "Remove Device"}
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </div>
      </div>
    </div>
  );
}
